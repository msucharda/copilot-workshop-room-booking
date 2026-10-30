package com.acme.roombooking.booking;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Booking policy")
class BookingPolicyController {
  private final BookingPolicy policy;

  BookingPolicyController(BookingPolicy policy) {
    this.policy = policy;
  }

  @GetMapping("/api/booking-policy")
  @Operation(summary = "Discover working hours, maximum duration and the advance booking window")
  PolicyView configuration() {
    var configuration = policy.configuration();
    return new PolicyView(
        configuration.zone().getId(),
        configuration.opensAt().toString(),
        configuration.closesAt().toString(),
        configuration.maximumDuration().toString(),
        configuration.advanceWindow().toString(),
        List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"));
  }

  record PolicyView(
      String zone,
      String opensAt,
      String closesAt,
      String maximumDuration,
      String advanceWindow,
      List<String> workingDays) {}
}
