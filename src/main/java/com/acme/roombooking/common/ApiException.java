package com.acme.roombooking.common;

import org.springframework.http.HttpStatus;

public final class ApiException extends RuntimeException {
  private final HttpStatus status;
  private final String code;

  public ApiException(HttpStatus status, String code, String message) {
    super(message);
    this.status = status;
    this.code = code;
  }

  public HttpStatus status() {
    return status;
  }

  public String code() {
    return code;
  }

  public static ApiException notFound(String resource) {
    return new ApiException(HttpStatus.NOT_FOUND, "not-found", resource + " was not found");
  }

  public static ApiException conflict(String code, String detail) {
    return new ApiException(HttpStatus.CONFLICT, code, detail);
  }

  public static ApiException invalid(String code, String detail) {
    return new ApiException(HttpStatus.BAD_REQUEST, code, detail);
  }
}
