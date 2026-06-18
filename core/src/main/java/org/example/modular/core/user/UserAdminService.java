package org.example.modular.core.user;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.example.modular.core.idp.IdpUserAdmin;
import org.example.modular.core.idp.IdpUserAdmin.RoleRef;
import org.example.modular.core.module.ModuleCatalog;
import org.example.modular.core.module.ModuleDTO;
import org.example.modular.core.module.ModuleDefinition;
import org.example.modular.core.module.ModuleService;
import org.example.modular.core.runtime.ModuleStatus;
import org.springframework.stereotype.Service;

/**
 * Backs the user-management UI: builds the assignable-role catalog from the realm admin role plus installed modules' client roles, lists users with their (catalog-restricted) roles, and
 * grants/revokes roles through the IdP.
 */
@Service
public class UserAdminService {

  private static final String PLATFORM_ADMIN = "platform-admin";

  private final ModuleCatalog catalog;
  private final ModuleService moduleService;
  private final IdpUserAdmin userAdmin;

  public UserAdminService(ModuleCatalog catalog, ModuleService moduleService, IdpUserAdmin userAdmin) {
    this.catalog = catalog;
    this.moduleService = moduleService;
    this.userAdmin = userAdmin;
  }

  /**
   * The matrix columns: the {@code platform-admin} realm role plus the client roles of every installed module (a module whose container exists, so its OAuth client has been provisioned).
   */
  public List<RoleDTO> assignableRoles() {
    List<RoleDTO> roles = new ArrayList<>();
    roles.add(new RoleDTO(PLATFORM_ADMIN, PLATFORM_ADMIN, null));
    for (ModuleDTO module : moduleService.list()) {
      if (module.status() == ModuleStatus.NOT_CREATED) {
        continue;
      }
      ModuleDefinition definition = catalog.byId(module.id());
      if (definition.getIdp() != null) {
        definition.getIdp().getRoles().forEach(role ->
            roles.add(new RoleDTO(module.id() + ":" + role, role, module.id())));
      }
    }
    return roles;
  }

  /**
   * All human users, each with the roles they hold that are manageable here (realm admin role + any module client role declared by a manifest); IdP noise like account/default roles is dropped.
   */
  public List<UserDTO> listUsers() {
    Set<String> manageable = manageableRoleIds();
    return userAdmin.listUsers().stream()
        .map(user -> new UserDTO(
            user.id(),
            user.username(),
            user.email(),
            user.roles().stream().map(UserAdminService::roleId).filter(manageable::contains).toList()))
        .toList();
  }

  public void grant(String userId, String roleId) {
    requireManageable(roleId);
    userAdmin.grantRole(userId, toRoleRef(roleId));
  }

  public void revoke(String userId, String roleId, String currentUserId) {
    requireManageable(roleId);
    // stop an admin from stripping their own platform-admin and locking themselves out
    if (PLATFORM_ADMIN.equals(roleId) && userId.equals(currentUserId)) {
      throw new IllegalArgumentException("You cannot remove your own platform-admin role.");
    }
    userAdmin.revokeRole(userId, toRoleRef(roleId));
  }

  /**
   * Validates against the manifest catalog (not container status), so a role for an uninstalled module is rejected here and a grant whose client is missing fails at the IdP.
   */
  private void requireManageable(String roleId) {
    if (!manageableRoleIds().contains(roleId)) {
      throw new IllegalArgumentException("Role is not assignable: " + roleId);
    }
  }

  private Set<String> manageableRoleIds() {
    Set<String> ids = new HashSet<>();
    ids.add(PLATFORM_ADMIN);
    for (ModuleDefinition definition : catalog.getModules()) {
      if (definition.getIdp() != null) {
        definition.getIdp().getRoles().forEach(role -> ids.add(definition.getId() + ":" + role));
      }
    }
    return ids;
  }

  private static String roleId(RoleRef role) {
    return role.module() == null ? role.name() : role.module() + ":" + role.name();
  }

  private static RoleRef toRoleRef(String roleId) {
    int separator = roleId.indexOf(':');
    return separator < 0
        ? new RoleRef(null, roleId)
        : new RoleRef(roleId.substring(0, separator), roleId.substring(separator + 1));
  }
}
