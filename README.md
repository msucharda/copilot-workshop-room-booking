# Workplace Room Booking

An internal meeting-room booking API for a multi-office company. Employees find
rooms by location, capacity and equipment, reserve a time slot, review their own
bookings and cancel future meetings. Facility administrators maintain the office
inventory. No clinical or personal contact data is stored.

The application has a production-oriented structure, not a production deployment
configuration. Local development accounts, HTTP and file-based H2 make it
self-contained. **Do not expose it to a network or use real company data.**
One known, non-security scheduling defect is deliberately retained in a
[bug task](.scratch/back-to-back-bookings/issues/01-fix-boundary-conflict.md).

## Prerequisites

- JDK **21**, with `JAVA_HOME` pointing at the JDK directory.
- A terminal and a Git clone. No Maven installation, database server or Docker.
- First-build access to Maven Central (`repo.maven.apache.org`) to download the
  pinned Maven distribution, plugins and application dependencies.

On Linux, keep `unzip` and `sha256sum` (or `shasum`) available so the stock wrapper
can extract and verify the pinned ZIP distribution. Ubuntu runners include them.

The application uses Spring Boot 4.1.2 and its managed JUnit Jupiter version.
The Apache Maven Wrapper is the `only-script` distribution; no wrapper JAR is
checked in.

## Build and run

Windows PowerShell:

```powershell
# Adjust this path to your JDK 21 installation.
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.12'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\mvnw.cmd verify
.\mvnw.cmd spring-boot:run
```

Linux:

```bash
# Point JAVA_HOME at your JDK 21 installation.
export PATH="$JAVA_HOME/bin:$PATH"
./mvnw verify
./mvnw spring-boot:run
```

Alternatively, run the packaged application after `verify`:

```text
java -jar target/room-booking-0.1.0-SNAPSHOT.jar
```

The service binds to **127.0.0.1:8080**. Stop it with Ctrl+C.

| Endpoint | Purpose |
|---|---|
| <http://localhost:8080/swagger-ui.html> | Interactive API documentation |
| <http://localhost:8080/v3/api-docs> | OpenAPI JSON |
| <http://localhost:8080/actuator/health> | Public liveness/dependency health |
| <http://localhost:8080/api/me> | Authenticated username and roles |
| <http://localhost:8080/api/booking-policy> | Current working hours and limits |

Swagger: authorize with a development account. For writes, first execute
`GET /api/csrf`, then paste its `token` into the `csrfToken` authorization field.
Use the same browser session: the token is paired with an HTTP session cookie.

### Development-only accounts

| Username | Password | Roles |
|---|---|---|
| `alice` | `alice-local-only` | `EMPLOYEE` |
| `bob` | `bob-local-only` | `EMPLOYEE` |
| `facilities` | `facilities-local-only` | `FACILITY_ADMIN`, `EMPLOYEE` |

These are public local credentials, not secrets. They are defined under
`booking.development.users` in `application.yml`; passwords are hashed in memory
at startup. Facility administrators inherit employee capabilities but **cannot
read or cancel another user's booking**. There is no impersonation parameter.

HTTP Basic authenticates every request. CSRF protection remains enabled because
browsers can automatically send Basic credentials. Read `/api/csrf`, retain its
session cookie, and send the returned header on every POST/PUT/PATCH/DELETE. Do not disable CSRF
to make a client work. H2 Console, user registration and remote management endpoints
are not enabled.

### Data and booking rules

Flyway creates the schema and seeds three offices, six floors and thirteen rooms.
Room 7 (`BA-HRON`) is out of service. New bookings start empty. Local data survives
restarts in `.data/room-booking.mv.db`; `.data/` is ignored by Git. Run one
application process per working directory. Worktrees have separate database files.
Tests use in-memory H2, not your local data.

- Booking timestamps are ISO-8601 instants with an offset, stored in UTC.
- Interval endpoints must be exactly representable at microsecond precision;
  trailing zeroes in fractional seconds are allowed. Finer precision returns 400.
- Office hours are Monday-Friday, **07:00-20:00 Europe/Bratislava**.
  Summer time follows the zone database. Public holidays are not modeled.
- A meeting must be in the future, last at most four hours, fit one working day
  and end within the next 90 days. The policy is configurable.
- Attendee count must fit the room. Inactive rooms cannot receive new bookings.
- Intended intervals are half-open: a meeting ending at 11:00 should allow another
  to start at 11:00. The known boundary defect currently rejects that case.
- Creation takes a database lock on the room before checking conflicts, so
  simultaneous requests cannot both reserve an overlapping slot.
- Only the owner can read or cancel a booking. Cancellation before the start is
  idempotent and preserves the record. Cancelled bookings no longer occupy a room.
- Inventory codes and room floor assignments are immutable. Updating a room's
  equipment, capacity or active state does not rewrite existing bookings.

### First booking from Windows PowerShell

Start the application in another terminal. This creates a meeting at 10:00 on the
next weekday, using the office time zone rather than your laptop's UTC offset.
If you already booked this slot, choose another room or a later weekday.

```powershell
$base = 'http://localhost:8080'
$basic = [Convert]::ToBase64String(
    [Text.Encoding]::UTF8.GetBytes('alice:alice-local-only'))
$headers = @{ Authorization = "Basic $basic" }
$csrf = Invoke-RestMethod "$base/api/csrf" -Headers $headers -SessionVariable bookingSession
$headers[$csrf.headerName] = $csrf.token

Invoke-RestMethod "$base/api/rooms?buildingId=1&minimumCapacity=8" -Headers $headers

$local = (Get-Date).Date.AddDays(1).AddHours(10)
while ($local.DayOfWeek -in @('Saturday', 'Sunday')) { $local = $local.AddDays(1) }
$zone = [TimeZoneInfo]::FindSystemTimeZoneById('Central Europe Standard Time')
$local = [DateTime]::SpecifyKind($local, [DateTimeKind]::Unspecified)
$start = [DateTimeOffset]::new($local, $zone.GetUtcOffset($local))
$end = $start.AddHours(1)
$body = @{
    roomId = 1; title = 'Delivery planning'; attendees = 6
    startsAt = $start.ToString('o'); endsAt = $end.ToString('o')
} | ConvertTo-Json
$created = Invoke-RestMethod "$base/api/bookings" -Method Post `
    -Headers $headers -WebSession $bookingSession -ContentType 'application/json' -Body $body
$created
Invoke-RestMethod "$base/api/bookings" -Headers $headers
```

Cancel it, when needed:

```powershell
Invoke-RestMethod "$base/api/bookings/$($created.id)/cancellation" -Method Post `
    -Headers $headers -WebSession $bookingSession
```

## API map

All `/api/**` routes require authentication. Inventory writes additionally require
`FACILITY_ADMIN`. Writes require the CSRF token described above.

| Routes | Capability |
|---|---|
| `GET /api/buildings`, `GET /api/buildings/{id}` | Browse office buildings |
| `POST /api/buildings`, `PUT /api/buildings/{id}` | Register and maintain buildings |
| `GET/POST /api/buildings/{id}/floors` | Browse or add floors |
| `GET /api/facility-summary` | Administrator-only room, seat and equipment inventory totals |
| `GET /api/rooms`, `GET /api/rooms/{id}` | Inventory; filter by building, floor, minimum capacity |
| `POST /api/rooms`, `PUT /api/rooms/{id}` | Register, maintain or deactivate rooms |
| `GET /api/availability` | Free rooms for an interval, location, capacity and equipment |
| `GET /api/rooms/{id}/schedule?date=YYYY-MM-DD` | Busy times for one office-local date; no private meeting details |
| `GET /api/bookings`, `GET /api/bookings/{id}` | Your private bookings |
| `POST /api/bookings` | Create a booking |
| `PATCH /api/bookings/{id}` | Replace the title and attendee count of your future confirmed booking |
| `POST /api/bookings/{id}/cancellation` | Cancel your future booking |

Booking lists use zero-based `page` (0-21474836), `size` (1-100, default 20), and optional
`status=CONFIRMED|CANCELLED`. The response contains `items`, `page`, `size`,
`totalElements` and `totalPages`. Room lists are ordered by code; availability
returns the smallest suitable rooms first, breaking ties by code. Availability is
a snapshot, not a reservation or guarantee.

Meeting room/time assignments cannot be edited in place. To move a meeting,
cancel the old booking and create a new one; these are separate operations and
the new slot is not held. Title/attendee edits retain the existing slot.

Errors use RFC 9457 `application/problem+json`. Domain errors have stable
`urn:room-booking:problem:<code>` types; validation errors include a `violations`
array. Client errors use 400 (invalid input/rules), 401 (authentication),
403 (permission/CSRF), 404 (missing resource) or 409 (conflict). Unexpected errors
are logged server-side and never include stack traces in the response.
Configurable duration limits in error details use complete ISO-8601 durations
(for example `PT30M` or `PT1H30M`), without truncating fractional hours.

See [API usage notes](docs/api-usage.md) for additional request examples.

## Code map and verification

Production code lives under `src/main/java/com/acme/roombooking`, organized by
feature:

| Package | Responsibility |
|---|---|
| `building` | Building/floor inventory and administrator operations |
| `room` | Room catalogue, equipment and per-room database lock |
| `booking` | Policy, ownership, conflict query and booking lifecycle |
| `availability` | Suitable free-room selection without private meeting data |
| `facility` | Administrator-only inventory totals by office |
| `security` | Development identities, role enforcement, CSRF and auth errors |
| `common` | ProblemDetail handling and paginated responses |
| `config` | Clock and OpenAPI configuration |

Controllers own HTTP/Bean Validation. Transactional services own use cases,
authorization and persistence coordination. JPA entities stay behind response
records; Open Session in View is disabled. Flyway migrations in
`src/main/resources/db/migration` own the schema; Hibernate validates it.

```powershell
.\mvnw.cmd spotless:apply
.\mvnw.cmd verify
.\mvnw.cmd '-Dtest=BookingPolicyTest,BookingApiIntegrationTest' test
```

Use the same Maven goals with `./mvnw` on Linux. `verify` runs the format check,
unit and `@SpringBootTest`/MockMvc integration tests, then builds an executable JAR.
Tests use a fixed clock; policy tests cover working hours and daylight saving,
while API tests cover persistence, validation, privacy, role/owner checks and CSRF.
GitHub Actions runs `verify` on both Windows and Linux with Java 21.

## Local task tracker

Six independent starting points live in `.scratch/`. Five specified tasks use
`<feature>/issues/01-<slug>.md`, with `Status: ready-for-agent` near the top and
conversation under `## Comments`. The larger feature is only an `idea.md`; its
specification and tickets do not exist yet. There is no Jira dependency.

| Type | Task | Primary change area |
|---|---|---|
| Bug | [Allow back-to-back bookings](.scratch/back-to-back-bookings/issues/01-fix-boundary-conflict.md) | `booking/BookingRepository.java` |
| Small feature | [Filter room inventory by equipment](.scratch/room-equipment-filter/issues/01-filter-room-equipment.md) | `room/RoomController.java`, `room/RoomService.java` |
| Small feature | [Export your bookings as CSV](.scratch/booking-csv-export/issues/01-export-own-bookings.md) | New `booking/export/` package |
| Bigger feature | [Recurring team meetings](.scratch/recurring-bookings/idea.md) | Raw product idea; scope undecided |
| Refactoring | [Separate availability selection steps](.scratch/availability-selection/issues/01-clarify-selection-pipeline.md) | `availability/AvailabilityService.java` |
| Documentation | [Document facility operations](.scratch/facility-api-guide/issues/01-document-facility-operations.md) | `docs/api-usage.md` |

The five specified tasks are designed for one focused 15-30 minute agent session
each. Their implementation and new-test files are disjoint; update only the
selected task's own status/comments, not this table or shared test helpers.
The bigger feature needs discovery before implementation. This repository does
not include generated assistant setup, domain docs or discovery artifacts.
