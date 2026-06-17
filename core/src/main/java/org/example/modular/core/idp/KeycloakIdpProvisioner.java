package org.example.modular.core.idp;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.example.modular.core.module.ModuleDefinition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Provisions module identity resources against the Keycloak Admin REST API, authenticated as the core client's service account (client credentials grant).
 */
@Slf4j
@Service
public class KeycloakIdpProvisioner implements IdpProvisioner {

  private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST_OF_MAPS = new ParameterizedTypeReference<>() {
  };
  private static final ParameterizedTypeReference<Map<String, Object>> MAP = new ParameterizedTypeReference<>() {
  };

  private final RestClient rest = RestClient.create();
  private final String issuerUri;
  private final String adminBase;
  private final String clientId;
  private final String clientSecret;

  public KeycloakIdpProvisioner(
      @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
      @Value("${spring.security.oauth2.client.registration.idp.client-id}") String clientId,
      @Value("${spring.security.oauth2.client.registration.idp.client-secret}") String clientSecret) {
    this.issuerUri = issuerUri;
    this.adminBase = issuerUri.replace("/realms/", "/admin/realms/");
    this.clientId = clientId;
    this.clientSecret = clientSecret;
  }

  /**
   * Reconciles the module's identity resources (OAuth client with a fresh secret, its client roles, service accounts) and returns the client id/secret env vars for the container.
   */
  @Override
  public Map<String, String> provision(ModuleDefinition module) {
    ModuleDefinition.Idp idp = module.getIdp();
    if (idp == null) {
      return Map.of();
    }
    log.info("Provisioning identity resources for module {}", module.getId());
    String token = adminToken();
    String secret = UUID.randomUUID().toString();
    String clientUuid = ensureClient(token, module.getId(), secret, idp.getRedirectUris());
    idp.getRoles().forEach(role -> ensureClientRole(token, clientUuid, role));
    idp.getUsers().forEach(user -> ensureUser(token, clientUuid, user));
    return Map.of(
        "MODULE_OIDC_CLIENT_ID", module.getId(),
        "MODULE_OIDC_CLIENT_SECRET", secret);
  }

  /**
   * Removes the module's identity resources: deleting the OAuth client cascades removal of its client roles and their user assignments, then the module's own user accounts are deleted, leaving
   * pre-existing platform users untouched.
   */
  @Override
  public void purge(ModuleDefinition module) {
    ModuleDefinition.Idp idp = module.getIdp();
    if (idp == null) {
      return;
    }
    log.info("Purging identity resources for module {}", module.getId());
    String token = adminToken();
    // deleting the client also removes its client roles and any user assignments of them
    findClient(token, module.getId()).ifPresent(internalId -> deleteQuietly(token, adminBase + "/clients/" + internalId));
    // users are realm-level, so the module's own service accounts must be removed explicitly
    idp.getUsers().forEach(user ->
        findUser(token, user.getUsername()).ifPresent(id -> deleteQuietly(token, adminBase + "/users/" + id)));
  }

  /**
   * Obtains a Keycloak admin access token via the core client's client-credentials grant.
   */
  private String adminToken() {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", "client_credentials");
    form.add("client_id", clientId);
    form.add("client_secret", clientSecret);
    Map<String, Object> response = rest.post()
        .uri(issuerUri + "/protocol/openid-connect/token")
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .body(form)
        .retrieve()
        .body(MAP);
    return (String) response.get("access_token");
  }

  /**
   * Creates the role on the module's OAuth client if absent, treating an already-existing role (HTTP 409) as success.
   */
  private void ensureClientRole(String token, String clientUuid, String role) {
    try {
      rest.post()
          .uri(adminBase + "/clients/" + clientUuid + "/roles")
          .headers(headers -> headers.setBearerAuth(token))
          .contentType(MediaType.APPLICATION_JSON)
          .body(Map.of("name", role))
          .retrieve()
          .toBodilessEntity();
      log.info("Created client role {}", role);
    } catch (RestClientResponseException e) {
      if (e.getStatusCode().value() != 409) {
        throw e;
      }
    }
  }

  /**
   * Creates the module's confidential OAuth client with a client-roles token mapper (or refreshes its secret and redirect URIs if it already exists) and returns its internal id.
   */
  private String ensureClient(String token, String moduleId, String secret, List<String> redirectUris) {
    Map<String, Object> representation = Map.of(
        "clientId", moduleId,
        "enabled", true,
        "publicClient", false,
        "secret", secret,
        "clientAuthenticatorType", "client-secret",
        "standardFlowEnabled", true,
        // service account: lets the module authenticate as itself (client-credentials) to call core, e.g. the event bus
        "serviceAccountsEnabled", true,
        "redirectUris", redirectUris);
    Optional<String> existing = findClient(token, moduleId);
    if (existing.isPresent()) {
      // reinstall: refresh secret and redirect uris (protocol mappers survive the update)
      rest.put()
          .uri(adminBase + "/clients/" + existing.get())
          .headers(headers -> headers.setBearerAuth(token))
          .contentType(MediaType.APPLICATION_JSON)
          .body(representation)
          .retrieve()
          .toBodilessEntity();
      return existing.get();
    }
    // the client-roles mapper exposes this client's roles under resource_access.<clientId>.roles for the module
    Map<String, Object> withMapper = new LinkedHashMap<>(representation);
    withMapper.put("protocolMappers", List.of(Map.of(
        "name", "client-roles",
        "protocol", "openid-connect",
        "protocolMapper", "oidc-usermodel-client-role-mapper",
        "config", Map.of(
            "usermodel.clientRoleMapping.clientId", moduleId,
            "claim.name", "resource_access." + moduleId + ".roles",
            "jsonType.label", "String",
            "multivalued", "true",
            "id.token.claim", "true",
            "access.token.claim", "true",
            "userinfo.token.claim", "true"))));
    rest.post()
        .uri(adminBase + "/clients")
        .headers(headers -> headers.setBearerAuth(token))
        .contentType(MediaType.APPLICATION_JSON)
        .body(withMapper)
        .retrieve()
        .toBodilessEntity();
    log.info("Created OAuth client {}", moduleId);
    return findClient(token, moduleId)
        .orElseThrow(() -> new IllegalStateException("Client not found after creation: " + moduleId));
  }

  /**
   * Ensures the module's service account exists (creating it with a generated password when missing) and assigns it the requested client roles.
   */
  private void ensureUser(String token, String clientUuid, ModuleDefinition.IdpUser user) {
    Optional<String> userId = findUser(token, user.getUsername());
    if (userId.isEmpty()) {
      String password = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
      rest.post()
          .uri(adminBase + "/users")
          .headers(headers -> headers.setBearerAuth(token))
          .contentType(MediaType.APPLICATION_JSON)
          .body(Map.of(
              "username", user.getUsername(),
              "enabled", true,
              "emailVerified", true,
              "credentials", List.of(Map.of("type", "password", "value", password, "temporary", false))))
          .retrieve()
          .toBodilessEntity();
      log.info("Created user {}", user.getUsername());
      userId = findUser(token, user.getUsername());
    }
    String id = userId.orElseThrow(() -> new IllegalStateException("User not found after creation: " + user.getUsername()));
    assignClientRoles(token, clientUuid, id, user.getRoles());
  }

  /**
   * Grants the given client roles to the user, first resolving each role's full representation as the Keycloak API requires.
   */
  private void assignClientRoles(String token, String clientUuid, String userId, List<String> roles) {
    if (roles.isEmpty()) {
      return;
    }
    List<Map<String, Object>> representations = roles.stream()
        .map(role -> rest.get()
            .uri(adminBase + "/clients/" + clientUuid + "/roles/{name}", role)
            .headers(headers -> headers.setBearerAuth(token))
            .retrieve()
            .body(MAP))
        .toList();
    rest.post()
        .uri(adminBase + "/users/" + userId + "/role-mappings/clients/" + clientUuid)
        .headers(headers -> headers.setBearerAuth(token))
        .contentType(MediaType.APPLICATION_JSON)
        .body(representations)
        .retrieve()
        .toBodilessEntity();
  }

  /**
   * Looks up a client's internal id by its clientId, or empty if none exists.
   */
  private Optional<String> findClient(String token, String moduleId) {
    List<Map<String, Object>> clients = rest.get()
        .uri(adminBase + "/clients?clientId={id}", moduleId)
        .headers(headers -> headers.setBearerAuth(token))
        .retrieve()
        .body(LIST_OF_MAPS);
    return clients == null || clients.isEmpty() ? Optional.empty() : Optional.of((String) clients.get(0).get("id"));
  }

  /**
   * Looks up a user's id by exact username, or empty if none exists.
   */
  private Optional<String> findUser(String token, String username) {
    List<Map<String, Object>> users = rest.get()
        .uri(adminBase + "/users?username={u}&exact=true", username)
        .headers(headers -> headers.setBearerAuth(token))
        .retrieve()
        .body(LIST_OF_MAPS);
    return users == null || users.isEmpty() ? Optional.empty() : Optional.of((String) users.get(0).get("id"));
  }

  /**
   * Sends a DELETE, ignoring a 404 so teardown stays idempotent.
   */
  private void deleteQuietly(String token, String uri) {
    try {
      rest.delete().uri(uri).headers(headers -> headers.setBearerAuth(token)).retrieve().toBodilessEntity();
    } catch (RestClientResponseException e) {
      if (e.getStatusCode().value() != 404) {
        throw e;
      }
    }
  }
}
