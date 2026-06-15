package org.example.modular.core.web;

import org.example.modular.core.module.ModuleNotFoundException;
import org.example.modular.core.provisioning.ModuleNotAuthorizedException;
import org.example.modular.core.runtime.InvalidModuleStateException;
import org.example.modular.core.runtime.RuntimeUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(RuntimeUnavailableException.class)
  public ProblemDetail handleRuntimeUnavailable(RuntimeUnavailableException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
  }

  @ExceptionHandler(ModuleNotAuthorizedException.class)
  public ProblemDetail handleNotAuthorized(ModuleNotAuthorizedException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
  }

  @ExceptionHandler(InvalidModuleStateException.class)
  public ProblemDetail handleInvalidState(InvalidModuleStateException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
  }

  @ExceptionHandler(ModuleNotFoundException.class)
  public ProblemDetail handleNotFound(ModuleNotFoundException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ProblemDetail handleBadRequest(IllegalArgumentException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
  }
}
