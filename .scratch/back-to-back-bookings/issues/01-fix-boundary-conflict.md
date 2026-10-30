# Allow back-to-back bookings

Status: ready-for-agent
Type: bug
Estimate: 15-25 minutes

## Problem

The conflict query treats interval boundaries as occupied. A meeting ending at
11:00 prevents a different meeting from starting at 11:00 in the same room.
The product rule is half-open intervals: `[start, end)`. This defect exists in the
current code and has no regression test.

## Reproduce

Use the README's PowerShell walkthrough against a fresh local database to create
the first meeting. Keep `$headers`, `$bookingSession`, `$start`, `$end` and
`$created`; do not cancel the first booking.

```powershell
$next = @{
    roomId = 1; title = 'Operations review'; attendees = 4
    startsAt = $end.ToString('o'); endsAt = $end.AddHours(1).ToString('o')
} | ConvertTo-Json
Invoke-RestMethod "$base/api/bookings" -Method Post `
    -Headers $headers -WebSession $bookingSession -ContentType 'application/json' -Body $next
```

Expected: HTTP 201. Actual: HTTP 409,
`urn:room-booking:problem:booking-conflict`. Free-room search for that same
following interval also omits room 1.

## Acceptance criteria

- A booking may start exactly when another confirmed booking ends.
- A booking may end exactly when another confirmed booking starts.
- An actual overlap, including containing/contained intervals, still returns 409.
- Availability and creation agree on the same interval semantics.
- Cancelled bookings remain non-blocking. Ownership and room locking do not change.
- Add regression coverage through the public HTTP API. The existing suite passes.

## Scope and validation

Change `src/main/java/com/acme/roombooking/booking/BookingRepository.java`.
Add `src/test/java/com/acme/roombooking/booking/BookingBoundaryIntegrationTest.java`;
reuse `ApiTestSupport` without editing it. Do not change other task areas.

Run `.\mvnw.cmd '-Dtest=BookingBoundaryIntegrationTest,BookingApiIntegrationTest,AvailabilityApiIntegrationTest' test`,
then `.\mvnw.cmd verify` (use `./mvnw` on Linux).

## Comments
