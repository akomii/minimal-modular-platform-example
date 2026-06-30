package org.example.modular.core.runtime;

/**
 * Lifecycle state of a module's container as reported by the {@link ModuleRuntime}.
 */
public enum ModuleStatus {
  RUNNING,
  STOPPED,
  /** No container exists for the module yet. */
  NOT_CREATED,
  /** The container exists but its state could not be read or mapped. */
  UNKNOWN
}
