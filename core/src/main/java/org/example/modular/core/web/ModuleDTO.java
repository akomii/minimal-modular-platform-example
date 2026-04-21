package org.example.modular.core.web;

import org.example.modular.core.module.ModuleDefinition;
import org.example.modular.core.runtime.ModuleRuntime;
import org.example.modular.core.runtime.ModuleStatus;

public record ModuleDTO(
    String id,
    String version,
    String type,
    ModuleStatus status
) {

  public static ModuleDTO from(ModuleDefinition module, ModuleRuntime runtime) {
    return new ModuleDTO(
        module.getId(),
        module.getVersion(),
        module.getType(),
        runtime.status(module)
    );
  }
}
