# API usage notes

The [README](../README.md) contains startup commands, development credentials,
CSRF handling and a complete booking example. OpenAPI at `/v3/api-docs` is the
source for request/response field definitions.

## Read-only requests

The following commands use curl (`curl.exe` in Windows PowerShell).

```bash
curl -u alice:alice-local-only http://localhost:8080/api/buildings
curl -u alice:alice-local-only http://localhost:8080/api/buildings/1/floors
curl -u alice:alice-local-only 'http://localhost:8080/api/rooms?buildingId=1&minimumCapacity=8'
curl -u alice:alice-local-only 'http://localhost:8080/api/bookings?page=0&size=20&status=CONFIRMED'
curl -u alice:alice-local-only http://localhost:8080/api/booking-policy
```

Do not put real passwords in shell history. These credentials are published
local-development fixtures only.

## Interpreting room inventory

Room codes uniquely identify a room across buildings. A floor has a numeric
`level` and a human-readable `name`. Room responses include their building and
floor identifiers so clients need not infer location from a room code.

`GET /api/rooms` hides inactive rooms by default. Pass `includeInactive=true` to
inspect the complete inventory. Individual inactive rooms remain readable by ID.
Equipment values are `DISPLAY`, `VIDEO_CONFERENCING`, `WHITEBOARD`, `PROJECTOR`
and `SPEAKERPHONE`.

Availability search requires `startsAt` and `endsAt`, supports repeated equipment
parameters, and requires all requested equipment. It never includes meeting
titles or owners. Choose an available room, then create the booking; another
client may reserve it between those requests.
