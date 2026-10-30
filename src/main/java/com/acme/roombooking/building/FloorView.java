package com.acme.roombooking.building;

public record FloorView(long id, long buildingId, int level, String name) {
  static FloorView from(Floor floor) {
    return new FloorView(
        floor.getId(), floor.getBuilding().getId(), floor.getLevel(), floor.getName());
  }
}
