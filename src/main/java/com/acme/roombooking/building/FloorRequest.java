package com.acme.roombooking.building;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FloorRequest(@Min(-5) @Max(100) int level, @NotBlank @Size(max = 100) String name) {}
