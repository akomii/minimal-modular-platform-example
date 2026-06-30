package org.example.modular.core.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Reads identity attributes from a Spring Security {@link Authentication}, working for both a BFF session login (an {@link OidcUser}) and a Bearer token (a {@link Jwt}).
 */
public final class AuthenticationPrincipals {

  private AuthenticationPrincipals() {
  }

  /**
   * The user's stable id: the token subject (the Keycloak user id).
   */
  public static String userId(Authentication authentication) {
    return switch (authentication.getPrincipal()) {
      case OidcUser oidc -> oidc.getSubject();
      case Jwt jwt -> jwt.getSubject();
      default -> authentication.getName();
    };
  }

  /**
   * The user's preferred username.
   */
  public static String username(Authentication authentication) {
    return switch (authentication.getPrincipal()) {
      case OidcUser oidc -> oidc.getPreferredUsername();
      case Jwt jwt -> jwt.getClaimAsString("preferred_username");
      default -> authentication.getName();
    };
  }
}
