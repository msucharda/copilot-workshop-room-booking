package com.acme.roombooking.building;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BuildingRequest(
    @NotBlank @Pattern(regexp = "[A-Z0-9][A-Z0-9-]{1,19}") String code,
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Size(max = 240) String address) {}
