package org.example.modular.core.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  /**
   * API clients (e.g. Postman) that send a {@code Authorization: Bearer <jwt>} header are authenticated statelessly as an OAuth2 resource server. This chain only matches such requests; everything
   * else falls through to the BFF chain below.
   */
  @Bean
  @Order(1)
  SecurityFilterChain apiTokenFilterChain(HttpSecurity http) throws Exception {
    http
        .securityMatcher(bearerTokenRequests())
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/user").authenticated()
            .requestMatchers("/api/modules/**").hasRole("PLATFORM_ADMIN")
            .anyRequest().authenticated())
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .csrf(csrf -> csrf.disable())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
    return http.build();
  }

  /**
   * Browser/SPA chain: confidential OAuth2 login (BFF), server-side session, CSRF.
   */
  @Bean
  @Order(2)
  SecurityFilterChain bffFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/", "/index.html", "/favicon.ico", "/assets/**", "/error").permitAll()
            // any authenticated user may read who they are (drives the SPA's login-vs-no-access UX)
            .requestMatchers("/api/user").authenticated()
            // the platform endpoints additionally require the platform-admin role
            .requestMatchers("/api/modules/**").hasRole("PLATFORM_ADMIN")
            .anyRequest().authenticated())
        .oauth2Login(Customizer.withDefaults())
        // local app logout: clear the server session and return 204 (no IdP round-trip)
        .logout(logout -> logout
            .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
        // API calls get a 401 they can react to, instead of a 302 redirect to Keycloak
        .exceptionHandling(ex -> ex.defaultAuthenticationEntryPointFor(
            new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
            PathPatternRequestMatcher.withDefaults().matcher("/api/**")))
        // CSRF for a cookie-based SPA — Spring Security 6 reference recipe
        .csrf(csrf -> csrf
            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
        .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class);
    return http.build();
  }

  /**
   * Routes only requests bearing an {@code Authorization: Bearer ...} header to the token chain.
   */
  private static RequestMatcher bearerTokenRequests() {
    return request -> {
      String header = request.getHeader(HttpHeaders.AUTHORIZATION);
      return header != null && header.regionMatches(true, 0, "Bearer ", 0, 7);
    };
  }

  /**
   * Maps Keycloak realm roles from a validated Bearer JWT into Spring authorities.
   */
  private static JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(jwt -> realmRoleAuthorities(jwt.getClaims()));
    return converter;
  }

  /**
   * Keycloak realm roles arrive in the {@code realm_access.roles} claim; the default OIDC login does not turn them into Spring authorities. Map each role to {@code ROLE_<UPPER_SNAKE>} so
   * {@code hasRole("PLATFORM_ADMIN")} matches the {@code platform-admin} realm role.
   */
  @Bean
  GrantedAuthoritiesMapper userAuthoritiesMapper() {
    return authorities -> {
      Set<GrantedAuthority> mapped = new HashSet<>(authorities);
      authorities.forEach(authority -> {
        if (authority instanceof OidcUserAuthority oidc) {
          mapped.addAll(realmRoleAuthorities(oidc.getIdToken().getClaims()));
          if (oidc.getUserInfo() != null) {
            mapped.addAll(realmRoleAuthorities(oidc.getUserInfo().getClaims()));
          }
        }
      });
      return mapped;
    };
  }

  private static Collection<GrantedAuthority> realmRoleAuthorities(Map<String, Object> claims) {
    Set<GrantedAuthority> authorities = new HashSet<>();
    if (claims.get("realm_access") instanceof Map<?, ?> realmAccess
        && realmAccess.get("roles") instanceof Collection<?> roles) {
      roles.forEach(role ->
          authorities.add(new SimpleGrantedAuthority(
              "ROLE_" + role.toString().toUpperCase(Locale.ROOT).replace('-', '_'))));
    }
    return authorities;
  }

  /**
   * Spring Security 6 SPA recipe: send the raw CSRF token (so the SPA can read the XSRF-TOKEN cookie and echo it in the X-XSRF-TOKEN header), but XOR-encode it when no header is present.
   */
  static final class SpaCsrfTokenRequestHandler extends CsrfTokenRequestAttributeHandler {

    private final CsrfTokenRequestHandler delegate = new XorCsrfTokenRequestAttributeHandler();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
      this.delegate.handle(request, response, csrfToken);
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
      if (StringUtils.hasText(request.getHeader(csrfToken.getHeaderName()))) {
        return super.resolveCsrfTokenValue(request, csrfToken);
      }
      return this.delegate.resolveCsrfTokenValue(request, csrfToken);
    }
  }

  /**
   * Forces the deferred CSRF token to be loaded so the XSRF-TOKEN cookie is written on every response.
   */
  static final class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
      CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
      csrfToken.getToken();
      filterChain.doFilter(request, response);
    }
  }
}
