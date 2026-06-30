package org.example.modular.core.runtime;

public class RuntimeUnavailableException extends RuntimeException {

  public RuntimeUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
