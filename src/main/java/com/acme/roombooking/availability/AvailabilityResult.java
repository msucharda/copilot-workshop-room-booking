package com.acme.roombooking.availability;

import com.acme.roombooking.room.RoomView;
import java.time.Instant;
import java.util.List;

public record AvailabilityResult(
    Instant startsAt, Instant endsAt, String zone, int minimumCapacity, List<RoomView> rooms) {}
