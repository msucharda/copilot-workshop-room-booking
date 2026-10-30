package com.acme.roombooking.booking;

import com.acme.roombooking.common.ApiException;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

@Component
public class BookingPolicy {
  private final BookingPolicyProperties properties;
  private final Clock clock;

  public BookingPolicy(BookingPolicyProperties properties, Clock clock) {
    this.properties = properties;
    this.clock = clock;
  }

  public BookingPolicyProperties configuration() {
    return properties;
  }

  public void validate(Instant startsAt, Instant endsAt) {
    if (startsAt == null || endsAt == null || !startsAt.isBefore(endsAt)) {
      throw ApiException.invalid("invalid-interval", "Start must be before end");
    }
    if (!startsAt.truncatedTo(ChronoUnit.MICROS).equals(startsAt)
        || !endsAt.truncatedTo(ChronoUnit.MICROS).equals(endsAt)) {
      throw ApiException.invalid(
          "unsupported-time-precision",
          "Booking times must be exactly representable at microsecond precision");
    }
    Instant now = clock.instant();
    if (startsAt.isBefore(now)) {
      throw ApiException.invalid("booking-in-past", "Bookings must start in the future");
    }
    if (Duration.between(startsAt, endsAt).compareTo(properties.maximumDuration()) > 0) {
      throw ApiException.invalid(
          "booking-too-long", "A booking cannot exceed " + properties.maximumDuration());
    }
    if (endsAt.isAfter(now.plus(properties.advanceWindow()))) {
      throw ApiException.invalid(
          "too-far-ahead", "Bookings must end within the advance booking window");
    }
    var localStart = startsAt.atZone(properties.zone());
    var localEnd = endsAt.atZone(properties.zone());
    if (!localStart.toLocalDate().equals(localEnd.toLocalDate())
        || localStart.getDayOfWeek() == DayOfWeek.SATURDAY
        || localStart.getDayOfWeek() == DayOfWeek.SUNDAY
        || localStart.toLocalTime().isBefore(properties.opensAt())
        || localEnd.toLocalTime().isAfter(properties.closesAt())) {
      throw ApiException.invalid(
          "outside-working-hours",
          "Bookings must fit a weekday between "
              + properties.opensAt()
              + " and "
              + properties.closesAt()
              + " in "
              + properties.zone());
    }
  }
}
