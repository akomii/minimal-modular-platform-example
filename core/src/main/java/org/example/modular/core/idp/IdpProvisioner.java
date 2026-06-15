package org.example.modular.core.idp;

import java.util.Map;
import org.example.modular.core.module.ModuleDefinition;

/**
 * Provisions the identity resources a module declares in its manifest (OAuth client, roles, users). Implementations are IdP-specific — swapping the identity provider means swapping the
 * implementation.
 */
public interface IdpProvisioner {

  /**
   * Creates the module's identity resources and returns the env vars (client id and generated secret) for the container. Empty for modules without an idp block.
   */
  Map<String, String> provision(ModuleDefinition module);

  /**
   * Removes all identity resources the module provisioned; a no-op for modules without an idp block.
   */
  void purge(ModuleDefinition module);
}
