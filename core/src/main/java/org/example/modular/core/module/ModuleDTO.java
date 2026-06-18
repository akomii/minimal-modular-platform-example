package org.example.modular.core.module;

import java.util.List;
import org.example.modular.core.runtime.ModuleStatus;

/**
 * API view of a module returned by the module endpoints.
 */
public record ModuleDTO(
    String id,
    String version,
    List<String> ports,
    ModuleStatus status,
    CoreAccess coreAccess,
    boolean authorized,
    List<ModuleDefinition.Dependency> dependsOn,
    List<ModuleDefinition.Endpoint> endpoints
) {

  /**
   * Builds a DTO from a module definition plus its current runtime status and authorization flag, defaulting core access to none when the module declares no database.
   */
  public static ModuleDTO from(ModuleDefinition module, ModuleStatus status, boolean authorized) {
    CoreAccess coreAccess = module.getDb() != null ? module.getDb().getCoreAccess() : CoreAccess.NONE;
    return new ModuleDTO(
        module.getId(),
        module.getVersion(),
        module.getPorts(),
        status,
        coreAccess,
        authorized,
        module.getDependsOn(),
        module.getEndpoints()
    );
  }
}
