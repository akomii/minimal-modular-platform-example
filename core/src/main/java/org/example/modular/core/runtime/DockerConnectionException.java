package org.example.modular.core.runtime;

public class DockerConnectionException extends RuntimeException {

  public DockerConnectionException(String message, Throwable cause) {
    super(message, cause);
  }
}
