package org.example.modular.core.config;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
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

/**
 * Defines the platform's HTTP security: a browser/SPA login chain (BFF session + CSRF) for the management UI, plus an optional stateless Bearer-token chain for API clients.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private static final String PLATFORM_ADMIN = "PLATFORM_ADMIN";

  private final IdpAuthoritiesMapper authoritiesMapper;

  public SecurityConfig(IdpAuthoritiesMapper authoritiesMapper) {
    this.authoritiesMapper = authoritiesMapper;
  }

  /**
   * Optional stateless chain that authenticates API clients sending an {@code Authorization: Bearer <jwt>} header as an OAuth2 resource server; enabled only when
   * {@code security.api-token.enabled=true}.
   */
  @Bean
  @Order(1)
  @ConditionalOnProperty(name = "security.api-token.enabled", havingValue = "true")
  SecurityFilterChain apiTokenFilterChain(HttpSecurity http) throws Exception {
    http.securityMatcher(bearerTokenRequests()).authorizeHttpRequests(auth -> {
          apiAuthorizationRules(auth);
          auth.anyRequest().authenticated();
        }).sessionManagement(session ->
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)).csrf(csrf ->
            csrf.disable())
        .oauth2ResourceServer(oauth2 ->
            oauth2.jwt(jwt ->
                jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
    return http.build();
  }

  /**
   * Default chain for the management UI: confidential OAuth2 login (backend-for-frontend), server-side session, and cookie-based CSRF.
   */
  @Bean
  @Order(2)
  SecurityFilterChain bffFilterChain(HttpSecurity http, ClientRegistrationRepository clientRegistrationRepository) throws Exception {
    http.authorizeHttpRequests(auth -> {
          // SSE streams are async requests; the initial dispatch is already authorized, so don't re-authorize the ASYNC/ERROR re-dispatch — otherwise an in-flight stream 403s once the session ends on logout
          auth.dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll();
          auth.requestMatchers("/", "/index.html", "/favicon.ico", "/assets/**", "/error").permitAll();
          apiAuthorizationRules(auth);
          auth.anyRequest().authenticated();
        }).oauth2Login(Customizer.withDefaults())
        // RP-initiated logout: end the Keycloak session too, not just the local one
        .logout(logout ->
            logout.logoutSuccessHandler(oidcLogoutSuccessHandler(clientRegistrationRepository)))
        // API calls get a 401 they can react to, instead of a 302 redirect to the IdP
        .exceptionHandling(ex ->
            ex.defaultAuthenticationEntryPointFor(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), PathPatternRequestMatcher.withDefaults().matcher("/api/**")))
        // CSRF for a cookie-based SPA — Spring Security 6 reference recipe
        .csrf(csrf ->
            csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()).csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
        .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class);
    return http.build();
  }

  /**
   * RP-initiated (single) logout: it ends the Keycloak session, not just the local one. Because the SPA triggers logout with a fetch (so the CSRF token rides in a header), it writes the IdP
   * end-session URL to the response body for the SPA to navigate to, instead of the 302 redirect a fetch would otherwise follow cross-origin to the IdP.
   */
  private LogoutSuccessHandler oidcLogoutSuccessHandler(ClientRegistrationRepository clientRegistrationRepository) {
    OidcClientInitiatedLogoutSuccessHandler handler = new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
    // Keycloak redirects here once its session is cleared; matches the core client's post.logout.redirect.uris
    handler.setPostLogoutRedirectUri("{baseUrl}/");
    handler.setRedirectStrategy((request, response, url) -> {
      response.setStatus(HttpStatus.OK.value());
      response.setContentType(MediaType.TEXT_PLAIN_VALUE);
      response.getWriter().write(url);
    });
    return handler;
  }

  /**
   * Shared authorization rules: any authenticated user may read {@code /api/user}; all module and observability endpoints additionally require the platform-admin role.
   */
  private static void apiAuthorizationRules(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
    auth
        // any authenticated user may read who they are and which module UIs they may see (drives the SPA's tabs and login-vs-no-access UX)
        .requestMatchers("/api/user", "/api/ui").authenticated()
        // modules (and admins) publish/subscribe on the event bus: any authenticated identity, no per-topic gate
        .requestMatchers("/api/events/**").authenticated()
        // the platform endpoints additionally require the platform-admin role
        .requestMatchers("/api/modules/**").hasRole(PLATFORM_ADMIN)
        // user/role administration is admin-only
        .requestMatchers("/api/users/**", "/api/roles").hasRole(PLATFORM_ADMIN)
        // observability streams (request log, server log) are admin-only too
        .requestMatchers("/api/requests/**", "/api/server/**").hasRole(PLATFORM_ADMIN)
        // demo-only database viewer exposes raw table contents — admin-only
        .requestMatchers("/api/db/**").hasRole(PLATFORM_ADMIN);
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
   * Converts the IdP roles in a validated Bearer JWT into Spring authorities for the API token chain.
   */
  private JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(jwt -> authoritiesMapper.fromClaims(jwt.getClaims()));
    return converter;
  }

  /**
   * Adds IdP roles as authorities after browser OIDC login, which Spring's default login does not map on its own.
   */
  @Bean
  GrantedAuthoritiesMapper userAuthoritiesMapper() {
    return authorities -> {
      Set<GrantedAuthority> mapped = new HashSet<>(authorities);
      authorities.forEach(authority -> {
        if (authority instanceof OidcUserAuthority oidc) {
          mapped.addAll(authoritiesMapper.fromClaims(oidc.getIdToken().getClaims()));
          if (oidc.getUserInfo() != null) {
            mapped.addAll(authoritiesMapper.fromClaims(oidc.getUserInfo().getClaims()));
          }
        }
      });
      return mapped;
    };
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
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
      CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
      csrfToken.getToken();
      filterChain.doFilter(request, response);
    }
  }
}
