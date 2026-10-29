# Filter room inventory by required equipment

Status: ready-for-agent
Type: feature
Estimate: 15-25 minutes

## User need

An employee browsing the inventory wants rooms with a whiteboard and a display
without choosing a meeting time. Availability search supports equipment; the
room catalogue currently filters only location and capacity.

## Acceptance criteria

- `GET /api/rooms?equipment=WHITEBOARD&equipment=DISPLAY` returns active rooms
  containing every requested equipment value.
- Omitted equipment keeps existing behavior. A duplicate value has no effect.
- Equipment combines with building, floor, capacity and `includeInactive` filters.
- An unknown value returns HTTP 400 ProblemDetail, not an empty success.
- Preserve code ordering, existing response fields and authorization.
- OpenAPI exposes the optional equipment parameter.
- Add API tests for all-of matching, combined filters, omission and unknown input.

## Scope and validation

Edit only `room/RoomController.java`, `room/RoomService.java` under the production
package. Reuse the existing `Equipment` enum; no schema change is needed.
Add `src/test/java/com/acme/roombooking/room/RoomEquipmentIntegrationTest.java`.
Do not change availability, shared test helpers or other task files.

Seed examples: `BA-DANUBE` has both requested values; `BA-LOBBY` has only a display;
`BA-FOCUS` has only a whiteboard.

Run `.\mvnw.cmd '-Dtest=RoomEquipmentIntegrationTest,InventoryApiIntegrationTest' test`,
then `.\mvnw.cmd verify` (use `./mvnw` on Linux).

## Comments
