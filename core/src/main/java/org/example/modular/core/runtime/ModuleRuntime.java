package org.example.modular.core.runtime;

import java.io.Closeable;
import org.example.modular.core.module.ModuleDefinition;

public interface ModuleRuntime {

  ModuleStatus status(ModuleDefinition module);

  void install(ModuleDefinition module);

  void start(ModuleDefinition module);

  void stop(ModuleDefinition module);

  void remove(ModuleDefinition module);

  Closeable streamLogs(ModuleDefinition module, LogSink sink);
}
