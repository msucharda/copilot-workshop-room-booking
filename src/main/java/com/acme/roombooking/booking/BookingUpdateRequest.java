package com.acme.roombooking.booking;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BookingUpdateRequest(
    @NotBlank @Size(max = 120) String title, @Min(1) @Max(200) int attendees) {}
