package org.example.modular.core.config;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * Turns the identity provider's roles (read from token claims via {@link IdpRoleExtractor}) into Spring authorities, so platform role checks like {@code hasRole("PLATFORM_ADMIN")} work for both
 * browser logins and Bearer-token API calls.
 */
@Component
public class IdpAuthoritiesMapper {

  private final IdpRoleExtractor roleExtractor;

  public IdpAuthoritiesMapper(IdpRoleExtractor roleExtractor) {
    this.roleExtractor = roleExtractor;
  }

  /**
   * Maps each IdP role in the given claims to a {@code ROLE_<UPPER_SNAKE>} authority (so {@code platform-admin} matches {@code hasRole("PLATFORM_ADMIN")}).
   */
  public Collection<GrantedAuthority> fromClaims(Map<String, Object> claims) {
    Set<GrantedAuthority> authorities = new HashSet<>();
    roleExtractor.extractRoles(claims).forEach(role ->
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase(Locale.ROOT).replace('-', '_'))));
    return authorities;
  }
}
