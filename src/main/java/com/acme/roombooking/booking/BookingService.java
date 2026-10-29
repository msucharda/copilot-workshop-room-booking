package com.acme.roombooking.booking;

import com.acme.roombooking.common.ApiException;
import com.acme.roombooking.common.PageResponse;
import com.acme.roombooking.room.Room;
import com.acme.roombooking.room.RoomRepository;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BookingService {
  private static final Logger LOG = LoggerFactory.getLogger(BookingService.class);
  private final BookingRepository bookings;
  private final RoomRepository rooms;
  private final BookingPolicy policy;
  private final Clock clock;

  public BookingService(
      BookingRepository bookings, RoomRepository rooms, BookingPolicy policy, Clock clock) {
    this.bookings = bookings;
    this.rooms = rooms;
    this.policy = policy;
    this.clock = clock;
  }

  @PreAuthorize("hasRole('EMPLOYEE') and #username == authentication.name")
  public PageResponse<BookingView> list(String username, BookingStatus status, int page, int size) {
    var order = Sort.by(Sort.Direction.DESC, "startsAt").and(Sort.by("id"));
    return PageResponse.from(
        bookings.findOwned(username, status, PageRequest.of(page, size, order)), BookingView::from);
  }

  @PreAuthorize("hasRole('EMPLOYEE') and #username == authentication.name")
  public BookingView get(UUID id, String username) {
    return BookingView.from(requireOwned(id, username));
  }

  @Transactional
  @PreAuthorize("hasRole('EMPLOYEE') and #username == authentication.name")
  public BookingView create(BookingRequest request, String username) {
    policy.validate(request.startsAt(), request.endsAt());
    Room room = rooms.lockById(request.roomId()).orElseThrow(() -> ApiException.notFound("Room"));
    if (!room.isActive()) {
      throw ApiException.conflict("room-unavailable", "This room is out of service");
    }
    if (request.attendees() > room.getCapacity()) {
      throw ApiException.invalid("capacity-exceeded", "The room does not have enough seats");
    }
    if (!bookings
        .findConflictingRoomIds(List.of(room.getId()), request.startsAt(), request.endsAt())
        .isEmpty()) {
      throw ApiException.conflict(
          "booking-conflict", "This room is already booked for the requested interval");
    }
    Booking booking = bookings.saveAndFlush(new Booking(room, username, request, clock.instant()));
    LOG.info("Booking {} created for room {}", booking.getId(), room.getCode());
    return BookingView.from(booking);
  }

  @Transactional
  @PreAuthorize("hasRole('EMPLOYEE') and #username == authentication.name")
  public BookingView cancel(UUID id, String username) {
    Booking booking = requireOwned(id, username);
    if (booking.getStatus() == BookingStatus.CANCELLED) {
      return BookingView.from(booking);
    }
    if (!booking.getStartsAt().isAfter(clock.instant())) {
      throw ApiException.conflict(
          "booking-already-started", "A booking cannot be cancelled after it starts");
    }
    booking.cancel(clock.instant());
    bookings.flush();
    LOG.info("Booking {} cancelled", booking.getId());
    return BookingView.from(booking);
  }

  private Booking requireOwned(UUID id, String username) {
    Booking booking = bookings.findById(id).orElseThrow(() -> ApiException.notFound("Booking"));
    if (!booking.getOwnerUsername().equals(username)) {
      throw new AccessDeniedException("Only the booking owner can access this booking");
    }
    return booking;
  }

  @Transactional
  @PreAuthorize("hasRole('EMPLOYEE') and #username == authentication.name")
  public BookingView updateDetails(UUID id, BookingUpdateRequest request, String username) {
    Booking booking = requireOwned(id, username);
    if (booking.getStatus() != BookingStatus.CONFIRMED
        || !booking.getStartsAt().isAfter(clock.instant())) {
      throw ApiException.conflict(
          "booking-not-editable", "Only future confirmed bookings can be edited");
    }
    Room room =
        rooms.lockById(booking.getRoom().getId()).orElseThrow(() -> ApiException.notFound("Room"));
    if (request.attendees() > room.getCapacity()) {
      throw ApiException.invalid("capacity-exceeded", "The room does not have enough seats");
    }
    booking.updateDetails(request);
    bookings.flush();
    LOG.info("Booking {} details updated", booking.getId());
    return BookingView.from(booking);
  }
}
