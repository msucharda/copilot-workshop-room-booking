package com.acme.roombooking.booking;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record BookingRequest(
    @Positive long roomId,
    @NotBlank @Size(max = 120) String title,
    @Min(1) @Max(200) int attendees,
    @NotNull Instant startsAt,
    @NotNull Instant endsAt) {}
