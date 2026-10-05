## Ticket

Closes #<!-- issue number -->

PRD story: <!-- e.g. TKT-1 -->

## What changed

<!-- 1–3 sentences: what this PR does and why. -->

-
-

## Acceptance criteria

<!-- Copy each criterion from the issue. Tick it and name the test that proves it. -->

- [ ] <!-- criterion --> — <!-- TestClass#method or test file -->

## How to test

1.
2.

## Screenshots

<!-- UI changes: before / after. Delete this section otherwise. -->

## Checklist

- [ ] One ticket per PR; branch named `<type>/<issue#>-<slug>`
- [ ] Tests added or updated; `/verify` passes (backend `./mvnw verify`, frontend lint, types, tests)
- [ ] New schema changes are in a new Flyway migration (no edits to applied ones)
- [ ] New endpoints have authorization checks and a test for a forbidden role
- [ ] No secrets, debug logs or commented-out code
- [ ] PRD updated if behaviour or API differs from it
