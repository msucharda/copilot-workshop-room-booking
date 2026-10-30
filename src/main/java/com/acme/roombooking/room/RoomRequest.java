package com.acme.roombooking.room;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record RoomRequest(
    @Positive long floorId,
    @NotBlank @Pattern(regexp = "[A-Z0-9][A-Z0-9-]{1,19}") String code,
    @NotBlank @Size(max = 100) String name,
    @Min(1) @Max(200) int capacity,
    boolean active,
    @NotNull @Size(max = 5) Set<@NotNull Equipment> equipment) {}
