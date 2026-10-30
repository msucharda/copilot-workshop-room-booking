package com.acme.roombooking.facility;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Facility operations", description = "Administrative inventory overview")
class FacilitySummaryController {
  private final FacilitySummaryService service;

  FacilitySummaryController(FacilitySummaryService service) {
    this.service = service;
  }

  @GetMapping("/api/facility-summary")
  @Operation(
      summary = "Get office inventory totals; requires FACILITY_ADMIN",
      description =
          "Available seats and equipment counts describe active inventory, "
              + "not current occupancy. No booking data is included.")
  FacilitySummary summary() {
    return service.get();
  }
}
