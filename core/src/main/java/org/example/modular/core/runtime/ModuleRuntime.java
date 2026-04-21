package org.example.modular.core.runtime;

import org.example.modular.core.module.ModuleDefinition;

public interface ModuleRuntime {

  ModuleStatus status(ModuleDefinition module);

  void start(ModuleDefinition module);

  void stop(ModuleDefinition module);
}
