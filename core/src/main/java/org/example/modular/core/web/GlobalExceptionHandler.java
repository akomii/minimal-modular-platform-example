package org.example.modular.core.web;

import org.example.modular.core.runtime.DockerConnectionException;
import org.example.modular.core.runtime.InvalidModuleStateException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(DockerConnectionException.class)
  public ResponseEntity<String> handleConnectionError(DockerConnectionException ex) {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body("Service Unavailable: " + ex.getMessage());
  }

  @ExceptionHandler(InvalidModuleStateException.class)
  public ResponseEntity<String> handleInvalidState(InvalidModuleStateException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body("Conflict: " + ex.getMessage());
  }
}
