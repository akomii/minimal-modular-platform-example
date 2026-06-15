package org.example.modular.core.module;

import org.example.modular.core.runtime.ModuleStatus;

/**
 * API view of a module returned by the module endpoints.
 */
public record ModuleDTO(
    String id,
    String version,
    String type,
    ModuleStatus status,
    CoreAccess coreAccess,
    boolean authorized
) {

  /**
   * Builds a DTO from a module definition plus its current runtime status and authorization flag, defaulting core access to none when the module declares no database.
   */
  public static ModuleDTO from(ModuleDefinition module, ModuleStatus status, boolean authorized) {
    CoreAccess coreAccess = module.getDb() != null ? module.getDb().getCoreAccess() : CoreAccess.NONE;
    return new ModuleDTO(
        module.getId(),
        module.getVersion(),
        module.getType(),
        status,
        coreAccess,
        authorized
    );
  }
}
