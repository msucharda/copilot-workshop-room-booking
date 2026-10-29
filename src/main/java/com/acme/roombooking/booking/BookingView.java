package com.acme.roombooking.booking;

import com.acme.roombooking.room.Room;
import java.time.Instant;
import java.util.UUID;

public record BookingView(
    UUID id,
    BookedRoom room,
    String ownerUsername,
    String title,
    int attendees,
    Instant startsAt,
    Instant endsAt,
    BookingStatus status,
    Instant createdAt,
    Instant cancelledAt) {
  static BookingView from(Booking booking) {
    Room room = booking.getRoom();
    return new BookingView(
        booking.getId(),
        new BookedRoom(
            room.getId(),
            room.getCode(),
            room.getName(),
            room.getFloor().getBuilding().getName(),
            room.getFloor().getLevel()),
        booking.getOwnerUsername(),
        booking.getTitle(),
        booking.getAttendees(),
        booking.getStartsAt(),
        booking.getEndsAt(),
        booking.getStatus(),
        booking.getCreatedAt(),
        booking.getCancelledAt());
  }

  public record BookedRoom(
      long id, String code, String name, String buildingName, int floorLevel) {}
}
