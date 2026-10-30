# Separate the availability selection steps

Status: ready-for-agent
Type: refactoring
Estimate: 15-25 minutes

## Problem

`AvailabilityService.search` handles policy validation, inventory retrieval,
location/capacity/equipment matching, conflict retrieval, mapping and ranking in
one method. It works, but the selection rules are hard to inspect independently
when another constraint is added.

## Acceptance criteria

- Keep the public method signature and the HTTP response unchanged.
- Give eligibility filtering, occupied-room exclusion and final ranking explicit
  names using small private methods in the same class. Avoid a new framework,
  strategy hierarchy or extra repository layer.
- Preserve active-only selection, all-of equipment matching, location filters,
  minimum capacity and capacity-then-code ordering.
- Keep policy validation before database work; do not query conflicts for an empty
  candidate set. Preserve the existing shared conflict query without fixing its
  boundary semantics in this task.
- Add behavior-preserving HTTP tests for an empty result, multiple equipment
  values and ordering. Existing authorization and API tests must still pass.

## Scope and validation

Change only `src/main/java/com/acme/roombooking/availability/AvailabilityService.java`.
Add `src/test/java/com/acme/roombooking/availability/AvailabilitySelectionIntegrationTest.java`.
Do not edit other task areas or the shared test support.

Run `.\mvnw.cmd '-Dtest=AvailabilitySelectionIntegrationTest,AvailabilityApiIntegrationTest' test`,
then `.\mvnw.cmd verify` (use `./mvnw` on Linux).

## Comments
