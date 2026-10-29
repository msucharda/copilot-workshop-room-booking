package com.acme.roombooking.booking;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.acme.roombooking.common.ApiException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BookingPolicyTest {
  private final BookingPolicy policy =
      new BookingPolicy(
          new BookingPolicyProperties(
              ZoneId.of("Europe/Bratislava"),
              LocalTime.of(7, 0),
              LocalTime.of(20, 0),
              Duration.ofHours(4),
              Duration.ofDays(90)),
          Clock.fixed(Instant.parse("2026-11-02T07:00:00Z"), ZoneId.of("UTC")));

  @Test
  void acceptsAWeekdayMeetingDuringOfficeHours() {
    assertThatCode(
            () ->
                policy.validate(
                    Instant.parse("2026-11-03T08:00:00Z"), Instant.parse("2026-11-03T09:00:00Z")))
        .doesNotThrowAnyException();
  }

  @ParameterizedTest
  @CsvSource({
    "2026-11-03T08:00:00Z,2026-11-03T08:00:00Z,invalid-interval",
    "2026-11-03T09:00:00Z,2026-11-03T08:00:00Z,invalid-interval",
    "2026-11-02T06:00:00Z,2026-11-02T08:00:00Z,booking-in-past",
    "2026-11-03T08:00:00Z,2026-11-03T12:01:00Z,booking-too-long",
    "2026-11-07T08:00:00Z,2026-11-07T09:00:00Z,outside-working-hours",
    "2026-11-08T08:00:00Z,2026-11-08T09:00:00Z,outside-working-hours",
    "2026-11-03T05:59:00Z,2026-11-03T07:00:00Z,outside-working-hours",
    "2026-11-03T18:00:00Z,2026-11-03T19:01:00Z,outside-working-hours",
    "2026-11-03T22:00:00Z,2026-11-04T01:00:00Z,outside-working-hours",
    "2027-02-02T08:00:00Z,2027-02-02T09:00:00Z,too-far-ahead"
  })
  void rejectsIntervalsOutsideTheBookingRules(String start, String end, String code) {
    assertThatThrownBy(() -> policy.validate(Instant.parse(start), Instant.parse(end)))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception ->
                org.assertj.core.api.Assertions.assertThat(exception.code()).isEqualTo(code));
  }

  @ParameterizedTest
  @CsvSource({
    "2026-11-03T06:00:00Z,2026-11-03T10:00:00Z",
    "2026-11-03T15:00:00Z,2026-11-03T19:00:00Z",
    "2027-01-29T08:00:00Z,2027-01-29T09:00:00Z"
  })
  void acceptsMaximumDurationAndExactWorkingHourLimits(String start, String end) {
    assertThatCode(() -> policy.validate(Instant.parse(start), Instant.parse(end)))
        .doesNotThrowAnyException();
  }

  @Test
  void summerOfficeHoursUseTheConfiguredTimeZoneRatherThanAFixedUtcOffset() {
    var summer =
        new BookingPolicy(
            policy.configuration(),
            Clock.fixed(Instant.parse("2026-07-01T04:00:00Z"), ZoneId.of("UTC")));
    assertThatCode(
            () ->
                summer.validate(
                    Instant.parse("2026-07-02T05:00:00Z"), Instant.parse("2026-07-02T06:00:00Z")))
        .doesNotThrowAnyException();
    assertThatThrownBy(
            () ->
                summer.validate(
                    Instant.parse("2026-07-02T04:59:00Z"), Instant.parse("2026-07-02T06:00:00Z")))
        .isInstanceOf(ApiException.class);
  }
}
