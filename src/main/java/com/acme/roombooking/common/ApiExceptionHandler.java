package com.acme.roombooking.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
  private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(ApiException.class)
  ProblemDetail handleApiException(ApiException exception, HttpServletRequest request) {
    return problem(exception.status(), exception.code(), exception.getMessage(), request);
  }

  @ExceptionHandler(AccessDeniedException.class)
  ProblemDetail handleAccessDenied(HttpServletRequest request) {
    return problem(HttpStatus.FORBIDDEN, "forbidden", "You cannot perform this action", request);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ProblemDetail handleIntegrity(
      DataIntegrityViolationException exception, HttpServletRequest request) {
    LOG.warn(
        "Conflicting inventory update on {}: {}",
        request.getRequestURI(),
        exception.getClass().getSimpleName());
    return problem(
        HttpStatus.CONFLICT,
        "duplicate-resource",
        "A resource with this code already exists",
        request);
  }

  @ExceptionHandler(PessimisticLockingFailureException.class)
  ProblemDetail handleBusy(HttpServletRequest request) {
    return problem(
        HttpStatus.CONFLICT, "room-busy", "The room is being updated; retry the request", request);
  }

  @ExceptionHandler(OptimisticLockingFailureException.class)
  ProblemDetail handleConcurrentUpdate(HttpServletRequest request) {
    return problem(
        HttpStatus.CONFLICT,
        "concurrent-update",
        "The resource changed during your request; reload it and retry",
        request);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  ProblemDetail handleConstraint(
      ConstraintViolationException exception, HttpServletRequest request) {
    ProblemDetail detail =
        problem(
            HttpStatus.BAD_REQUEST, "validation-failed", "Request parameters are invalid", request);
    detail.setProperty(
        "violations",
        exception.getConstraintViolations().stream()
            .map(
                violation ->
                    Map.of(
                        "field",
                        violation.getPropertyPath().toString(),
                        "message",
                        violation.getMessage()))
            .toList());
    return detail;
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException exception,
      org.springframework.http.HttpHeaders headers,
      org.springframework.http.HttpStatusCode status,
      WebRequest request) {
    ProblemDetail detail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request fields are invalid");
    detail.setType(URI.create("urn:room-booking:problem:validation-failed"));
    detail.setTitle("Validation failed");
    List<Map<String, String>> violations =
        exception.getBindingResult().getFieldErrors().stream()
            .map(
                error ->
                    Map.of(
                        "field",
                        error.getField(),
                        "message",
                        error.getDefaultMessage() == null
                            ? "Invalid value"
                            : error.getDefaultMessage()))
            .toList();
    detail.setProperty("violations", violations);
    return handleExceptionInternal(exception, detail, headers, status, request);
  }

  @ExceptionHandler(Exception.class)
  ProblemDetail handleUnexpected(Exception exception, HttpServletRequest request) {
    LOG.error("Unhandled request failure on {}", request.getRequestURI(), exception);
    return problem(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "internal-error",
        "An unexpected error occurred",
        request);
  }

  private ProblemDetail problem(
      HttpStatus status, String code, String message, HttpServletRequest request) {
    ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, message);
    detail.setType(URI.create("urn:room-booking:problem:" + code));
    detail.setInstance(URI.create(request.getRequestURI()));
    return detail;
  }
}
