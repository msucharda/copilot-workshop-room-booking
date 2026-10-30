package com.acme.roombooking.booking;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("booking.policy")
public record BookingPolicyProperties(
    @NotNull ZoneId zone,
    @NotNull LocalTime opensAt,
    @NotNull LocalTime closesAt,
    @NotNull Duration maximumDuration,
    @NotNull Duration advanceWindow) {
  public BookingPolicyProperties {
    if (opensAt != null && closesAt != null && !opensAt.isBefore(closesAt)) {
      throw new IllegalArgumentException("Opening time must be before closing time");
    }
    if (maximumDuration != null && (maximumDuration.isNegative() || maximumDuration.isZero())) {
      throw new IllegalArgumentException("Maximum duration must be positive");
    }
    if (advanceWindow != null && (advanceWindow.isNegative() || advanceWindow.isZero())) {
      throw new IllegalArgumentException("Advance window must be positive");
    }
  }
}
