package com.acme.roombooking.facility;

import com.acme.roombooking.building.BuildingService;
import com.acme.roombooking.building.BuildingView;
import com.acme.roombooking.building.Floor;
import com.acme.roombooking.building.FloorRepository;
import com.acme.roombooking.facility.FacilitySummary.BuildingSummary;
import com.acme.roombooking.room.Equipment;
import com.acme.roombooking.room.Room;
import com.acme.roombooking.room.RoomRepository;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FacilitySummaryService {
  private final BuildingService buildings;
  private final FloorRepository floors;
  private final RoomRepository rooms;

  public FacilitySummaryService(
      BuildingService buildings, FloorRepository floors, RoomRepository rooms) {
    this.buildings = buildings;
    this.floors = floors;
    this.rooms = rooms;
  }

  @PreAuthorize("hasRole('FACILITY_ADMIN')")
  public FacilitySummary get() {
    List<BuildingView> offices = buildings.list();
    Map<Long, List<Floor>> floorsByBuilding =
        floors.findAll().stream()
            .collect(Collectors.groupingBy(floor -> floor.getBuilding().getId()));
    Map<Long, List<Room>> roomsByBuilding =
        rooms.findCatalog().stream()
            .collect(Collectors.groupingBy(room -> room.getFloor().getBuilding().getId()));
    List<BuildingSummary> summaries =
        offices.stream()
            .map(
                building ->
                    summarize(
                        building,
                        floorsByBuilding.getOrDefault(building.id(), List.of()).size(),
                        roomsByBuilding.getOrDefault(building.id(), List.of())))
            .toList();
    return new FacilitySummary(
        offices.size(),
        summaries.stream().mapToInt(BuildingSummary::floorCount).sum(),
        summaries.stream().mapToInt(BuildingSummary::activeRoomCount).sum(),
        summaries.stream().mapToInt(BuildingSummary::inactiveRoomCount).sum(),
        summaries.stream().mapToInt(BuildingSummary::availableSeats).sum(),
        summaries);
  }

  private BuildingSummary summarize(BuildingView building, int floorCount, List<Room> inventory) {
    List<Room> active = inventory.stream().filter(Room::isActive).toList();
    Map<String, Long> equipmentCounts = new TreeMap<>();
    for (Equipment equipment : Equipment.values()) {
      long count = active.stream().filter(room -> room.getEquipment().contains(equipment)).count();
      equipmentCounts.put(equipment.name(), count);
    }
    return new BuildingSummary(
        building.id(),
        building.code(),
        building.name(),
        floorCount,
        active.size(),
        inventory.size() - active.size(),
        active.stream().mapToInt(Room::getCapacity).sum(),
        Map.copyOf(equipmentCounts));
  }
}
