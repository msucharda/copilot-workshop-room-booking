package com.acme.roombooking.availability;

import com.acme.roombooking.booking.BusyInterval;
import com.acme.roombooking.room.RoomView;
import java.time.LocalDate;
import java.util.List;

public record RoomScheduleView(
    RoomView room,
    LocalDate date,
    String zone,
    boolean workingDay,
    String opensAt,
    String closesAt,
    List<BusyInterval> busyIntervals) {}
