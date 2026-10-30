package com.acme.roombooking.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("booking.development")
public record DevelopmentUsers(@NotEmpty List<@Valid User> users) {
  public record User(
      @NotBlank @Pattern(regexp = "[a-z][a-z0-9._-]{1,49}") String username,
      @NotBlank String password,
      @NotBlank @Pattern(regexp = "EMPLOYEE|FACILITY_ADMIN") String role) {}
}
