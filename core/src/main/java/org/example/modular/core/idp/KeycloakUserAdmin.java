package org.example.modular.core.idp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

/**
 * Reads and edits user role assignments through the Keycloak Admin REST API, authenticated as the core client's service account.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "idp.provider", havingValue = "keycloak", matchIfMissing = true)
public class KeycloakUserAdmin implements IdpUserAdmin {

  private final KeycloakAdminClient admin;

  public KeycloakUserAdmin(KeycloakAdminClient admin) {
    this.admin = admin;
  }

  @Override
  public List<DirectoryUser> listUsers() {
    String token = admin.token();
    List<Map<String, Object>> users = admin.rest().get()
        .uri(admin.adminBase() + "/users?max=1000")
        .headers(headers -> headers.setBearerAuth(token))
        .retrieve()
        .body(KeycloakAdminClient.LIST_OF_MAPS);
    if (users == null) {
      return List.of();
    }
    return users.stream()
        // service accounts are machine identities (username "service-account-<client>"), not platform users
        .filter(user -> !String.valueOf(user.get("username")).startsWith("service-account-"))
        .map(user -> toDirectoryUser(token, user))
        .toList();
  }

  @Override
  public List<RoleRef> rolesOf(String userId) {
    return roleRefs(admin.token(), userId);
  }

  private DirectoryUser toDirectoryUser(String token, Map<String, Object> user) {
    String id = (String) user.get("id");
    return new DirectoryUser(id, (String) user.get("username"), (String) user.get("email"), roleRefs(token, id));
  }

  /**
   * Fetches a user's combined realm + client role mappings (one call) and maps them to provider-agnostic role refs.
   */
  private List<RoleRef> roleRefs(String token, String userId) {
    Map<String, Object> mappings = admin.rest().get()
        .uri(admin.adminBase() + "/users/" + userId + "/role-mappings")
        .headers(headers -> headers.setBearerAuth(token))
        .retrieve()
        .body(KeycloakAdminClient.MAP);
    List<RoleRef> roles = new ArrayList<>();
    if (mappings != null) {
      if (mappings.get("realmMappings") instanceof List<?> realm) {
        realm.forEach(role -> roles.add(new RoleRef(null, roleName(role))));
      }
      if (mappings.get("clientMappings") instanceof Map<?, ?> clients) {
        clients.forEach((clientId, value) -> {
          if (value instanceof Map<?, ?> mapping && mapping.get("mappings") instanceof List<?> assigned) {
            assigned.forEach(role -> roles.add(new RoleRef(String.valueOf(clientId), roleName(role))));
          }
        });
      }
    }
    return roles;
  }

  private static String roleName(Object roleRepresentation) {
    return roleRepresentation instanceof Map<?, ?> map ? String.valueOf(map.get("name")) : String.valueOf(roleRepresentation);
  }

  @Override
  public void grantRole(String userId, RoleRef role) {
    applyRole(userId, role, true);
  }

  @Override
  public void revokeRole(String userId, RoleRef role) {
    applyRole(userId, role, false);
  }

  /**
   * Resolves the role's full representation (as the Keycloak API requires) and adds or removes it from the user's realm or client role mappings.
   */
  private void applyRole(String userId, RoleRef role, boolean grant) {
    String token = admin.token();
    if (role.module() == null) {
      Map<String, Object> representation = admin.rest().get()
          .uri(admin.adminBase() + "/roles/{name}", role.name())
          .headers(headers -> headers.setBearerAuth(token))
          .retrieve()
          .body(KeycloakAdminClient.MAP);
      sendRoleMapping(token, grant, admin.adminBase() + "/users/" + userId + "/role-mappings/realm", representation);
    } else {
      String clientUuid = admin.findClient(token, role.module())
          .orElseThrow(() -> new IllegalArgumentException("No identity client for module: " + role.module()));
      Map<String, Object> representation = admin.rest().get()
          .uri(admin.adminBase() + "/clients/" + clientUuid + "/roles/{name}", role.name())
          .headers(headers -> headers.setBearerAuth(token))
          .retrieve()
          .body(KeycloakAdminClient.MAP);
      sendRoleMapping(token, grant, admin.adminBase() + "/users/" + userId + "/role-mappings/clients/" + clientUuid, representation);
    }
  }

  private void sendRoleMapping(String token, boolean grant, String uri, Map<String, Object> roleRepresentation) {
    List<Map<String, Object>> body = List.of(roleRepresentation);
    admin.rest()
        .method(grant ? HttpMethod.POST : HttpMethod.DELETE)
        .uri(uri)
        .headers(headers -> headers.setBearerAuth(token))
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)
        .retrieve()
        .toBodilessEntity();
  }
}
