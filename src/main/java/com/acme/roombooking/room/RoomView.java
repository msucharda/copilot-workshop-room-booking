package com.acme.roombooking.room;

import java.util.List;

public record RoomView(
    long id,
    String code,
    String name,
    int capacity,
    boolean active,
    long buildingId,
    String buildingName,
    long floorId,
    int floorLevel,
    String floorName,
    List<Equipment> equipment) {
  public static RoomView from(Room room) {
    var floor = room.getFloor();
    var building = floor.getBuilding();
    return new RoomView(
        room.getId(),
        room.getCode(),
        room.getName(),
        room.getCapacity(),
        room.isActive(),
        building.getId(),
        building.getName(),
        floor.getId(),
        floor.getLevel(),
        floor.getName(),
        room.getEquipment().stream().sorted().toList());
  }
}
