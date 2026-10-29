# Document the facility administrator workflow

Status: ready-for-agent
Type: documentation
Estimate: 15-20 minutes

## Reader need

A facility administrator can discover endpoints in Swagger but has no end-to-end
guide for registering an office and taking a room out of service. The current
usage notes contain only read-only examples and an inventory explanation.

## Acceptance criteria

- Extend `docs/api-usage.md` with a runnable Windows PowerShell workflow that
  authenticates as `facilities`, obtains a CSRF token and preserves its cookie.
- Create a building, a floor and a room, carrying returned IDs into the next call.
- Update the room to inactive, then show that normal catalogue/availability
  searches exclude it but lookup by ID still works.
- Explain immutable codes/floor assignments and that deactivation preserves
  existing bookings. Do not promise administrative access to private bookings.
- Include one expected 403 employee example and one 400 validation example,
  describing the important ProblemDetail fields.
- Use only development credentials and relative links to the README/OpenAPI.
  Explain how to choose a new unique code when repeating the workflow.
- Check every request against the running API or integration tests. No workshop
  facilitation or assistant-setup instructions belong in this document.

## Scope and validation

Change only `docs/api-usage.md` and this task's own status/comments.
No application or shared README edits are needed. Build the baseline with
`.\mvnw.cmd verify` and follow the documented requests locally (use `./mvnw` on
Linux). Do not commit generated databases, cookies or temporary output.

## Comments
