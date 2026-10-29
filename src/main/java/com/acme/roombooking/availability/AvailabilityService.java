package com.acme.roombooking.availability;

import com.acme.roombooking.booking.BookingPolicy;
import com.acme.roombooking.booking.BookingRepository;
import com.acme.roombooking.room.Equipment;
import com.acme.roombooking.room.Room;
import com.acme.roombooking.room.RoomRepository;
import com.acme.roombooking.room.RoomView;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AvailabilityService {
  private final RoomRepository rooms;
  private final BookingRepository bookings;
  private final BookingPolicy policy;

  public AvailabilityService(
      RoomRepository rooms, BookingRepository bookings, BookingPolicy policy) {
    this.rooms = rooms;
    this.bookings = bookings;
    this.policy = policy;
  }

  public AvailabilityResult search(
      Instant startsAt,
      Instant endsAt,
      Long buildingId,
      Long floorId,
      int minimumCapacity,
      Set<Equipment> equipment) {
    policy.validate(startsAt, endsAt);
    List<Room> inventory = rooms.findCatalog();
    List<Room> candidates = new ArrayList<>();
    for (Room room : inventory) {
      if (!room.isActive()) {
        continue;
      }
      if (buildingId != null && !buildingId.equals(room.getFloor().getBuilding().getId())) {
        continue;
      }
      if (floorId != null && !floorId.equals(room.getFloor().getId())) {
        continue;
      }
      if (room.getCapacity() < minimumCapacity) {
        continue;
      }
      if (!room.getEquipment().containsAll(equipment)) {
        continue;
      }
      candidates.add(room);
    }

    Set<Long> occupied = new HashSet<>();
    if (!candidates.isEmpty()) {
      List<Long> ids = candidates.stream().map(Room::getId).toList();
      occupied.addAll(bookings.findConflictingRoomIds(ids, startsAt, endsAt));
    }

    List<RoomView> available = new ArrayList<>();
    for (Room room : candidates) {
      if (!occupied.contains(room.getId())) {
        available.add(RoomView.from(room));
      }
    }
    available.sort(Comparator.comparingInt(RoomView::capacity).thenComparing(RoomView::code));
    return new AvailabilityResult(
        startsAt,
        endsAt,
        policy.configuration().zone().getId(),
        minimumCapacity,
        List.copyOf(available));
  }
}
