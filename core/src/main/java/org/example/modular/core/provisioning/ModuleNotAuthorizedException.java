package org.example.modular.core.provisioning;

public class ModuleNotAuthorizedException extends RuntimeException {

  public ModuleNotAuthorizedException(String message) {
    super(message);
  }
}
