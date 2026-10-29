package com.acme.roombooking.availability;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@Tag(name = "Availability")
class RoomScheduleController {
  private final RoomScheduleService service;

  RoomScheduleController(RoomScheduleService service) {
    this.service = service;
  }

  @GetMapping("/api/rooms/{id}/schedule")
  @Operation(
      summary = "Get a room's busy intervals for one office-local calendar date",
      description =
          "Includes only confirmed intervals. No booking IDs, titles or owners are exposed. "
              + "Inactive rooms remain readable; workingDay refers to the weekday, not bookability.")
  RoomScheduleView get(@PathVariable @Positive long id, @RequestParam LocalDate date) {
    return service.get(id, date);
  }
}
