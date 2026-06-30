package org.example.modular.core.user;

import java.util.List;
import org.example.modular.core.config.AuthenticationPrincipals;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin API for the user-management UI: list users and the assignable role catalog, and grant/revoke a single role. Gated to platform-admin in {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api")
public class UserAdminController {

  private final UserAdminService service;

  public UserAdminController(UserAdminService service) {
    this.service = service;
  }

  @GetMapping("/users")
  public List<UserDTO> users() {
    return service.listUsers();
  }

  @GetMapping("/roles")
  public List<RoleDTO> roles() {
    return service.assignableRoles();
  }

  @PutMapping("/users/{userId}/roles/{roleId}")
  public void grant(@PathVariable String userId, @PathVariable String roleId) {
    service.grant(userId, roleId);
  }

  @DeleteMapping("/users/{userId}/roles/{roleId}")
  public void revoke(@PathVariable String userId, @PathVariable String roleId, Authentication authentication) {
    // pass the caller's id so the service can block self-revocation of platform-admin
    service.revoke(userId, roleId, AuthenticationPrincipals.userId(authentication));
  }
}
