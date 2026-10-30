package com.acme.roombooking.room;

import com.acme.roombooking.building.Floor;
import com.acme.roombooking.building.FloorRepository;
import com.acme.roombooking.common.ApiException;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RoomService {
  private final RoomRepository rooms;
  private final FloorRepository floors;

  public RoomService(RoomRepository rooms, FloorRepository floors) {
    this.rooms = rooms;
    this.floors = floors;
  }

  public List<RoomView> list(
      Long buildingId, Long floorId, int minimumCapacity, boolean includeInactive) {
    return rooms.findCatalog().stream()
        .filter(room -> includeInactive || room.isActive())
        .filter(
            room -> buildingId == null || room.getFloor().getBuilding().getId().equals(buildingId))
        .filter(room -> floorId == null || room.getFloor().getId().equals(floorId))
        .filter(room -> room.getCapacity() >= minimumCapacity)
        .map(RoomView::from)
        .toList();
  }

  public RoomView get(long id) {
    return RoomView.from(rooms.findById(id).orElseThrow(() -> ApiException.notFound("Room")));
  }

  @Transactional
  @PreAuthorize("hasRole('FACILITY_ADMIN')")
  public RoomView create(RoomRequest request) {
    Floor floor =
        floors.findById(request.floorId()).orElseThrow(() -> ApiException.notFound("Floor"));
    return RoomView.from(rooms.saveAndFlush(new Room(floor, request)));
  }

  @Transactional
  @PreAuthorize("hasRole('FACILITY_ADMIN')")
  public RoomView update(long id, RoomRequest request) {
    Room room = rooms.lockById(id).orElseThrow(() -> ApiException.notFound("Room"));
    if (!room.getCode().equals(request.code()) || room.getFloor().getId() != request.floorId()) {
      throw ApiException.invalid(
          "immutable-location", "Room codes and floor assignments cannot be changed");
    }
    room.update(request);
    return RoomView.from(rooms.saveAndFlush(room));
  }
}
