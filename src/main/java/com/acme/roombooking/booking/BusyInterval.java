package com.acme.roombooking.booking;

import java.time.Instant;

public record BusyInterval(Instant startsAt, Instant endsAt) {}
