package org.example.modular.core.runtime;

public class DockerConnectionException extends RuntimeUnavailableException {

  public DockerConnectionException(String message, Throwable cause) {
    super(message, cause);
  }
}
