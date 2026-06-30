package org.example.modular.core.idp;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Shared Keycloak Admin REST plumbing — the admin base URL, a service-account admin token (client-credentials grant), and client lookup — reused by the Keycloak IdP implementations.
 */
@Component
@ConditionalOnProperty(name = "idp.provider", havingValue = "keycloak", matchIfMissing = true)
public class KeycloakAdminClient {

  // shared by the Keycloak IdP implementations in this package to read the admin API's JSON object / array responses
  static final ParameterizedTypeReference<List<Map<String, Object>>> LIST_OF_MAPS = new ParameterizedTypeReference<>() {
  };
  static final ParameterizedTypeReference<Map<String, Object>> MAP = new ParameterizedTypeReference<>() {
  };

  private final RestClient rest = RestClient.create();
  private final String issuerUri;
  private final String adminBase;
  private final String clientId;
  private final String clientSecret;

  public KeycloakAdminClient(
      @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
      @Value("${spring.security.oauth2.client.registration.idp.client-id}") String clientId,
      @Value("${spring.security.oauth2.client.registration.idp.client-secret}") String clientSecret) {
    this.issuerUri = issuerUri;
    this.adminBase = issuerUri.replace("/realms/", "/admin/realms/");
    this.clientId = clientId;
    this.clientSecret = clientSecret;
  }

  public RestClient rest() {
    return rest;
  }

  public String adminBase() {
    return adminBase;
  }

  /**
   * Obtains a Keycloak admin access token via the core client's client-credentials grant.
   */
  public String token() {
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
   * Looks up a client's internal id by its clientId, or empty if none exists.
   */
  public Optional<String> findClient(String token, String clientId) {
    List<Map<String, Object>> clients = rest.get()
        .uri(adminBase + "/clients?clientId={id}", clientId)
        .headers(headers -> headers.setBearerAuth(token))
        .retrieve()
        .body(LIST_OF_MAPS);
    return clients == null || clients.isEmpty() ? Optional.empty() : Optional.of((String) clients.get(0).get("id"));
  }
}
