# Issue Tracker

Web app for projects, tickets, a Kanban board, assignees and comments.
Product spec: `docs/PRODUCT_REQUIREMENTS.md` — the source of truth for scope, story IDs (AUTH-1, TKT-3, …), domain model and API outline.

## Stack and layout

```
backend/    Spring Boot 3, Java 21, Maven wrapper, Spring Data JPA, Flyway, PostgreSQL 16
frontend/   React 18, TypeScript, Vite, TanStack Query, Tailwind, dnd-kit
docs/       PRD and design notes
.github/    PR and issue templates, CI
```

Backend packages are by feature, not by layer:
`com.issuetracker.{auth,project,ticket,board,comment,notification,common}` — each holds its controller, service, repository, entity, DTOs.

## Commands

| Task | Command |
| --- | --- |
| Start Postgres | `docker compose up -d db` |
| Backend run | `cd backend && ./mvnw spring-boot:run` |
| Backend tests | `cd backend && ./mvnw test` |
| Backend full check | `cd backend && ./mvnw verify` |
| Frontend dev | `cd frontend && npm run dev` |
| Frontend tests | `cd frontend && npm test -- --run` |
| Frontend lint + types | `cd frontend && npm run lint && npx tsc --noEmit` |

If a directory or script does not exist yet, say so instead of inventing output.

## Workflow

- **Tickets are GitHub Issues.** Each issue maps to a PRD story ID in its title, e.g. `[TKT-1] Create a ticket`.
- **Branches:** `<type>/<issue#>-<short-slug>`, e.g. `feat/12-kanban-drag-drop`. Types: `feat`, `fix`, `chore`, `docs`, `refactor`, `test`.
- **Commits:** Conventional Commits with the issue number: `feat(board): move cards between columns (#12)`.
- **PRs:** one ticket per PR, body follows `.github/pull_request_template.md`, includes `Closes #<issue>`.
- Never commit or push directly to `main`.

## Project skills

| Skill | Use it to |
| --- | --- |
| `/create-tickets` | Turn PRD stories into GitHub issues |
| `/implement-ticket <issue#>` | Branch, plan, test-first implement, commit |
| `/verify` | Run every build, lint and test check and report results |
| `/review-ticket <pr#\|branch>` | Review a change against its ticket's acceptance criteria |
| `/raise-pr` | Verify, push and open a PR from the template |
| `/db-migration <description>` | Add a Flyway migration that matches the entities |
| `/api-endpoint <METHOD path>` | Add a REST endpoint end to end with tests |

## Code conventions

**Backend**
- Controllers are thin: validate input (`@Valid` DTOs), call a service, map to a response DTO. Never return entities.
- Business rules and `@Transactional` live in services.
- Authorization with `@PreAuthorize` on service or controller methods; every endpoint has a test for an unauthorized role.
- Errors as RFC 7807 `ProblemDetail` via one `@RestControllerAdvice`. Stale `@Version` → `409 Conflict`.
- Schema changes only through new Flyway migrations (`V<n>__<snake_description>.sql`); never edit an applied one. `spring.jpa.hibernate.ddl-auto=validate`.
- Tests: JUnit 5 + Mockito for services; `@SpringBootTest` + Testcontainers Postgres for repositories and API tests.

**Frontend**
- Server state through TanStack Query hooks in `src/api/`; no fetch calls inside components.
- Feature folders under `src/features/<feature>/`.
- Board moves are optimistic and roll back on error.
- Render user Markdown only through the sanitising renderer.
- Tests: Vitest + React Testing Library; query by role and label, not test IDs.

## Definition of done

1. Every acceptance criterion in the issue is met and covered by a test.
2. `/verify` passes.
3. No secrets, debug logging or commented-out code.
4. API changes are reflected in the PRD API outline if they differ from it.
