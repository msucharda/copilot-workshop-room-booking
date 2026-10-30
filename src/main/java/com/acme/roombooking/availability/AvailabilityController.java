package com.acme.roombooking.availability;

import com.acme.roombooking.room.Equipment;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Set;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/availability")
@Tag(
    name = "Availability",
    description = "Find bookable rooms without exposing anyone's meeting details")
class AvailabilityController {
  private final AvailabilityService service;

  AvailabilityController(AvailabilityService service) {
    this.service = service;
  }

  @GetMapping
  @Operation(
      summary = "Find free rooms, smallest suitable capacity first",
      description =
          "Availability is a snapshot, not a hold. POST /api/bookings rechecks conflicts atomically.")
  AvailabilityResult search(
      @RequestParam Instant startsAt,
      @RequestParam Instant endsAt,
      @RequestParam(required = false) @Positive Long buildingId,
      @RequestParam(required = false) @Positive Long floorId,
      @RequestParam(defaultValue = "1") @Min(1) @Max(200) int minimumCapacity,
      @RequestParam(required = false) @Size(min = 1) Set<@NotNull Equipment> equipment) {
    return service.search(
        startsAt,
        endsAt,
        buildingId,
        floorId,
        minimumCapacity,
        equipment == null ? Set.of() : equipment);
  }
}
