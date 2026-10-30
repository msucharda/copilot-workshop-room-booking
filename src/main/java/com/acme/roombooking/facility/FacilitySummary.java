package com.acme.roombooking.facility;

import java.util.List;
import java.util.Map;

public record FacilitySummary(
    int buildingCount,
    int floorCount,
    int activeRoomCount,
    int inactiveRoomCount,
    int availableSeats,
    List<BuildingSummary> buildings) {

  public record BuildingSummary(
      long id,
      String code,
      String name,
      int floorCount,
      int activeRoomCount,
      int inactiveRoomCount,
      int availableSeats,
      Map<String, Long> equippedRoomCounts) {}
}
