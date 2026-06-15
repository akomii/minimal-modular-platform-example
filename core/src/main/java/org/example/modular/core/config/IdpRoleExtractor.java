package org.example.modular.core.config;

import java.util.Collection;
import java.util.Map;

/**
 * Extracts the role names a user holds from the identity provider's token claims (JWT, ID token or userinfo). Where roles live in the token is the one provider-specific part of the security
 * setup — swapping the identity provider means providing a different implementation of this interface; {@link SecurityConfig} stays untouched.
 */
public interface IdpRoleExtractor {

  /**
   * Returns the role names present in the given token claims, or empty if none are found.
   */
  Collection<String> extractRoles(Map<String, Object> claims);
}
