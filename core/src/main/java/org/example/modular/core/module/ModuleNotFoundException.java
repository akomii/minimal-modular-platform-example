package org.example.modular.core.module;

public class ModuleNotFoundException extends RuntimeException {

  public ModuleNotFoundException(String message) {
    super(message);
  }
}
