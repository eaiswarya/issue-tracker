---
name: verify
description: Run every build, lint, type-check and test for backend and frontend and report pass/fail with the real output. Use before committing, before raising a PR, when asked "does it build", "run the tests", "check everything", or invoke /verify.
argument-hint: "[backend | frontend | all]"
---

# Verify

Scope: `$ARGUMENTS` (default `all`). Run only the parts that exist; skip a missing `backend/` or `frontend/` and say it was skipped.

## Backend (`backend/`)

```bash
cd backend && ./mvnw -q verify
```
- Runs compile, unit tests and Testcontainers integration tests. Docker must be running; if it is not, say so. Don't report the integration tests as passed.
- On failure: show the failing test names and the first assertion or stack frame from each, from `target/surefire-reports` / `target/failsafe-reports`.

## Frontend (`frontend/`)

```bash
cd frontend && npm run lint && npx tsc --noEmit && npm test -- --run && npm run build
```
Run the four steps separately so one failure doesn't hide the rest.

## Extra checks on the diff vs `origin/main`

- Edited an existing Flyway migration? `git diff --name-status origin/main...HEAD -- backend/src/main/resources/db/migration` shows `M` → fail.
- Secrets: grep added lines for `password=`, `secret`, `apiKey`, `BEGIN PRIVATE KEY`, `.env` files → flag.
- Leftover debug: `System.out.println`, `console.log`, `debugger`, `.only(` in added lines → flag.

## Report

```
| Check | Result | Notes |
| Backend verify | pass / FAIL / skipped | 142 tests, 0 failures |
| Frontend lint | ... |
| Frontend types | ... |
| Frontend tests | ... |
| Frontend build | ... |
| Migrations / secrets / debug | ... |
```

Report only what you ran and saw. Never write "pass" for a step that didn't run. Don't fix anything unless asked; list the failures and suggest fixes.
