package org.example.modular.core.config;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Keycloak implementation of {@link IdpRoleExtractor}: reads roles from the token's {@code realm_access.roles} claim.
 */
@Component
public class KeycloakRoleExtractor implements IdpRoleExtractor {

  /**
   * Returns the roles under {@code realm_access.roles}, or empty if that claim is absent or malformed.
   */
  @Override
  public Collection<String> extractRoles(Map<String, Object> claims) {
    if (claims.get("realm_access") instanceof Map<?, ?> realmAccess
        && realmAccess.get("roles") instanceof Collection<?> roles) {
      return roles.stream().map(Object::toString).toList();
    }
    return List.of();
  }
}
