package org.example.modular.core.runtime;

public enum ModuleStatus {
  RUNNING,
  STOPPED,
  NOT_CREATED,
  UNKNOWN;

  public static ModuleStatus fromDockerState(String state) {
    if (state == null) {
      return UNKNOWN;
    }
    return switch (state.toLowerCase()) {
      case "running" -> RUNNING;
      case "exited", "created", "dead" -> STOPPED;
      default -> UNKNOWN;
    };
  }
}
