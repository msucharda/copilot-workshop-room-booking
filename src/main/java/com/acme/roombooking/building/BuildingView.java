package com.acme.roombooking.building;

public record BuildingView(long id, String code, String name, String address) {
  static BuildingView from(Building building) {
    return new BuildingView(
        building.getId(), building.getCode(), building.getName(), building.getAddress());
  }
}
