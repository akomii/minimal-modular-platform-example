package org.example.modular.core.module;

import org.example.modular.core.runtime.ModuleStatus;

public record ModuleDTO(
    String id,
    String version,
    String type,
    ModuleStatus status,
    CoreAccess coreAccess,
    boolean authorized
) {

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
