package com.acme.roombooking.availability;

import com.acme.roombooking.booking.BookingPolicy;
import com.acme.roombooking.booking.BookingRepository;
import com.acme.roombooking.common.ApiException;
import com.acme.roombooking.room.RoomRepository;
import com.acme.roombooking.room.RoomView;
import java.time.DayOfWeek;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RoomScheduleService {
  private final RoomRepository rooms;
  private final BookingRepository bookings;
  private final BookingPolicy policy;

  public RoomScheduleService(
      RoomRepository rooms, BookingRepository bookings, BookingPolicy policy) {
    this.rooms = rooms;
    this.bookings = bookings;
    this.policy = policy;
  }

  public RoomScheduleView get(long roomId, LocalDate date) {
    var room = rooms.findById(roomId).orElseThrow(() -> ApiException.notFound("Room"));
    var configuration = policy.configuration();
    var zone = configuration.zone();
    var dayStartsAt = date.atStartOfDay(zone).toInstant();
    var dayEndsAt = date.plusDays(1).atStartOfDay(zone).toInstant();
    var intervals = bookings.findBusyIntervals(roomId, dayStartsAt, dayEndsAt);
    boolean workingDay =
        date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY;
    return new RoomScheduleView(
        RoomView.from(room),
        date,
        zone.getId(),
        workingDay,
        configuration.opensAt().toString(),
        configuration.closesAt().toString(),
        intervals);
  }
}
