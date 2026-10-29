package com.acme.roombooking.building;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/buildings")
@Tag(
    name = "Buildings",
    description = "Building and floor inventory; writes require FACILITY_ADMIN")
class BuildingController {
  private final BuildingService service;

  BuildingController(BuildingService service) {
    this.service = service;
  }

  @GetMapping
  @Operation(summary = "List buildings in code order")
  List<BuildingView> list() {
    return service.list();
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get a building")
  BuildingView get(@PathVariable @Positive long id) {
    return service.get(id);
  }

  @PostMapping
  @Operation(summary = "Register a building")
  ResponseEntity<BuildingView> create(@Valid @RequestBody BuildingRequest request) {
    BuildingView building = service.create(request);
    return ResponseEntity.created(URI.create("/api/buildings/" + building.id())).body(building);
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update building details; codes are immutable")
  BuildingView update(
      @PathVariable @Positive long id, @Valid @RequestBody BuildingRequest request) {
    return service.update(id, request);
  }

  @GetMapping("/{id}/floors")
  @Operation(summary = "List floors from lowest to highest")
  List<FloorView> floors(@PathVariable @Positive long id) {
    return service.floors(id);
  }

  @PostMapping("/{id}/floors")
  @Operation(summary = "Add a floor to a building")
  ResponseEntity<FloorView> addFloor(
      @PathVariable @Positive long id, @Valid @RequestBody FloorRequest request) {
    FloorView floor = service.addFloor(id, request);
    return ResponseEntity.created(URI.create("/api/buildings/" + id + "/floors")).body(floor);
  }
}
