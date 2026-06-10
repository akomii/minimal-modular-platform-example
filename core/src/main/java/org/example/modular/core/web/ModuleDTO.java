package org.example.modular.core.web;

import org.example.modular.core.module.ModuleDefinition;
import org.example.modular.core.runtime.ModuleStatus;

public record ModuleDTO(
    String id,
    String version,
    String type,
    ModuleStatus status,
    String coreAccess,
    boolean authorized
) {

  public static ModuleDTO from(ModuleDefinition module, ModuleStatus status, boolean authorized) {
    String coreAccess = module.getDb() != null ? module.getDb().getCoreAccess() : "none";
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
