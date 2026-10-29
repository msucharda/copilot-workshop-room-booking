package com.acme.roombooking.building;

import com.acme.roombooking.common.ApiException;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BuildingService {
  private final BuildingRepository buildings;
  private final FloorRepository floors;

  public BuildingService(BuildingRepository buildings, FloorRepository floors) {
    this.buildings = buildings;
    this.floors = floors;
  }

  public List<BuildingView> list() {
    return buildings.findAllByOrderByCodeAsc().stream().map(BuildingView::from).toList();
  }

  public BuildingView get(long id) {
    return BuildingView.from(requireBuilding(id));
  }

  public List<FloorView> floors(long buildingId) {
    requireBuilding(buildingId);
    return floors.findByBuildingIdOrderByLevelAsc(buildingId).stream()
        .map(FloorView::from)
        .toList();
  }

  @Transactional
  @PreAuthorize("hasRole('FACILITY_ADMIN')")
  public BuildingView create(BuildingRequest request) {
    return BuildingView.from(
        buildings.saveAndFlush(new Building(request.code(), request.name(), request.address())));
  }

  @Transactional
  @PreAuthorize("hasRole('FACILITY_ADMIN')")
  public BuildingView update(long id, BuildingRequest request) {
    Building building = requireBuilding(id);
    if (!building.getCode().equals(request.code())) {
      throw ApiException.invalid("immutable-code", "Building codes cannot be changed");
    }
    building.update(request.name(), request.address());
    return BuildingView.from(buildings.saveAndFlush(building));
  }

  @Transactional
  @PreAuthorize("hasRole('FACILITY_ADMIN')")
  public FloorView addFloor(long buildingId, FloorRequest request) {
    Building building = requireBuilding(buildingId);
    return FloorView.from(
        floors.saveAndFlush(new Floor(building, request.level(), request.name())));
  }

  private Building requireBuilding(long id) {
    return buildings.findById(id).orElseThrow(() -> ApiException.notFound("Building"));
  }
}
