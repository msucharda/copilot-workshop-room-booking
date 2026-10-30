package com.acme.roombooking.room;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/rooms")
@Tag(name = "Rooms", description = "Room inventory; writes require FACILITY_ADMIN")
class RoomController {
  private final RoomService service;

  RoomController(RoomService service) {
    this.service = service;
  }

  @GetMapping
  @Operation(summary = "Browse rooms by location and capacity")
  List<RoomView> list(
      @RequestParam(required = false) @Positive Long buildingId,
      @RequestParam(required = false) @Positive Long floorId,
      @RequestParam(defaultValue = "1") @Min(1) @Max(200) int minimumCapacity,
      @RequestParam(defaultValue = "false") boolean includeInactive) {
    return service.list(buildingId, floorId, minimumCapacity, includeInactive);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get a room, including inactive rooms")
  RoomView get(@PathVariable @Positive long id) {
    return service.get(id);
  }

  @PostMapping
  @Operation(summary = "Register a room")
  ResponseEntity<RoomView> create(@Valid @RequestBody RoomRequest request) {
    RoomView room = service.create(request);
    return ResponseEntity.created(URI.create("/api/rooms/" + room.id())).body(room);
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update a room or take it out of service; existing bookings are preserved")
  RoomView update(@PathVariable @Positive long id, @Valid @RequestBody RoomRequest request) {
    return service.update(id, request);
  }
}
