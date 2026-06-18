package org.example.modular.core.idp;

import java.util.List;

/**
 * Provider-agnostic administration of platform users and their role assignments, backing the user-management UI. Implementations are IdP-specific — swapping the identity provider means swapping the
 * implementation.
 */
public interface IdpUserAdmin {

  /**
   * All human users in the realm (service accounts excluded), each with their currently assigned roles (realm and client).
   */
  List<DirectoryUser> listUsers();

  /**
   * The roles (realm and client) currently assigned to the given user.
   */
  List<RoleRef> rolesOf(String userId);

  /**
   * Grants the role to the user; idempotent.
   */
  void grantRole(String userId, RoleRef role);

  /**
   * Revokes the role from the user; idempotent.
   */
  void revokeRole(String userId, RoleRef role);

  /**
   * A user as seen by the admin UI: the IdP's internal id, the username, an optional email, and the roles currently assigned.
   */
  record DirectoryUser(String id, String username, String email, List<RoleRef> roles) {

  }

  /**
   * A reference to a role: a realm role when {@code module} is null, otherwise a client role on that module's OAuth client.
   */
  record RoleRef(String module, String name) {

  }
}
