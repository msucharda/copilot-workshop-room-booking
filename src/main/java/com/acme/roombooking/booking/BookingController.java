package com.acme.roombooking.booking;

import com.acme.roombooking.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.security.Principal;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/bookings")
@Tag(
    name = "Bookings",
    description = "Private bookings; only the authenticated owner can read or cancel them")
class BookingController {
  private final BookingService service;

  BookingController(BookingService service) {
    this.service = service;
  }

  @GetMapping
  @Operation(summary = "List your bookings, newest start first")
  PageResponse<BookingView> list(
      Principal principal,
      @RequestParam(required = false) BookingStatus status,
      @RequestParam(defaultValue = "0") @Min(0) @Max(BookingService.MAX_PAGE_INDEX) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(BookingService.MAX_PAGE_SIZE) int size) {
    return service.list(principal.getName(), status, page, size);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get one of your bookings")
  BookingView get(@PathVariable UUID id, Principal principal) {
    return service.get(id, principal.getName());
  }

  @PostMapping
  @Operation(summary = "Book a room; authenticated identity determines ownership")
  ResponseEntity<BookingView> create(
      @Valid @RequestBody BookingRequest request, Principal principal) {
    BookingView booking = service.create(request, principal.getName());
    return ResponseEntity.created(URI.create("/api/bookings/" + booking.id())).body(booking);
  }

  @PostMapping("/{id}/cancellation")
  @Operation(summary = "Cancel your future booking; repeating a cancellation is safe")
  BookingView cancel(@PathVariable UUID id, Principal principal) {
    return service.cancel(id, principal.getName());
  }

  @PatchMapping("/{id}")
  @Operation(
      summary = "Replace the title and attendee count of your future confirmed booking",
      description =
          "Room and time are immutable. Cancel and create a new booking to move a meeting.")
  BookingView updateDetails(
      @PathVariable UUID id,
      @Valid @RequestBody BookingUpdateRequest request,
      Principal principal) {
    return service.updateDetails(id, request, principal.getName());
  }
}
