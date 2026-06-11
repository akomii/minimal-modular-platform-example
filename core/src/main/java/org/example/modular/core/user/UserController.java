package org.example.modular.core.user;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

  @GetMapping
  public UserInfo current(Authentication authentication) {
    List<String> roles = authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .filter(authority -> authority.startsWith("ROLE_"))
        .map(authority -> authority.substring("ROLE_".length()))
        .toList();
    return new UserInfo(username(authentication), roles);
  }

  // Session login (BFF) yields an OidcUser; a Bearer token yields a Jwt — read the name from either.
  private static String username(Authentication authentication) {
    return switch (authentication.getPrincipal()) {
      case OidcUser oidc -> oidc.getPreferredUsername();
      case Jwt jwt -> jwt.getClaimAsString("preferred_username");
      default -> authentication.getName();
    };
  }

  public record UserInfo(String username, List<String> roles) {

  }
}
