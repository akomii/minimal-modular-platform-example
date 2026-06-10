package org.example.modular.core.db;

public class ModuleNotAuthorizedException extends RuntimeException {

  public ModuleNotAuthorizedException(String message) {
    super(message);
  }
}
