package org.example.modular.core.user;

import java.util.List;
import org.example.modular.core.config.AuthenticationPrincipals;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
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
    return new UserInfo(AuthenticationPrincipals.username(authentication), roles);
  }

  public record UserInfo(String username, List<String> roles) {

  }
}
