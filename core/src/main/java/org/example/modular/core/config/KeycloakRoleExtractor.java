package org.example.modular.core.config;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class KeycloakRoleExtractor implements IdpRoleExtractor {

  @Override
  public Collection<String> extractRoles(Map<String, Object> claims) {
    if (claims.get("realm_access") instanceof Map<?, ?> realmAccess
        && realmAccess.get("roles") instanceof Collection<?> roles) {
      return roles.stream().map(Object::toString).toList();
    }
    return List.of();
  }
}
