# Export one page of your bookings as CSV

Status: ready-for-agent
Type: feature
Estimate: 20-30 minutes

## User need

An employee wants to save a page of their bookings for a spreadsheet without
copying JSON by hand. Export uses the existing private, paginated booking list.

## Acceptance criteria

- Add authenticated `GET /api/booking-exports.csv`.
- Accept the existing optional status, zero-based page (0-21474836, default 0) and size
  (1-100, default 20). Export only that page, in the existing list order.
- Always derive identity from the authenticated principal. Reuse
  `BookingService.list`; do not add repository access or an owner query parameter.
- Return `text/csv;charset=UTF-8` and
  `Content-Disposition: attachment; filename="my-bookings.csv"`.
- Header order is `id,roomCode,title,startsAt,endsAt,status`.
  Times are ISO-8601 UTC strings. Use CRLF record separators.
- Quote all data fields, escape embedded quotes by doubling them, and preserve
  commas/newlines. Prefix spreadsheet-formula text with an apostrophe when its
  first non-whitespace character is `=`, `+`, `-` or `@`, or it begins with a tab.
- An empty page returns only the header. Invalid filters return ProblemDetail.
- Test content type, quoting, formula neutralization, empty output, pagination,
  authentication and the impossibility of exporting another user's bookings.
- Document the new operation with OpenAPI annotations on the new controller.

## Scope and validation

Create implementation files only in
`src/main/java/com/acme/roombooking/booking/export/` and an API test at
`src/test/java/com/acme/roombooking/booking/export/BookingExportIntegrationTest.java`.
The existing service is public and returns `PageResponse<BookingView>`.
Reuse its `MAX_PAGE_INDEX` and `MAX_PAGE_SIZE` validation limits.
No schema, security configuration, README or shared test helper edits are needed.

Run `.\mvnw.cmd '-Dtest=BookingExportIntegrationTest,AuthorizationIntegrationTest' test`,
then `.\mvnw.cmd verify` (use `./mvnw` on Linux).

## Comments
