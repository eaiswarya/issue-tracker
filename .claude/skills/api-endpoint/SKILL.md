---
name: api-endpoint
description: Add a REST endpoint end to end — DTOs, controller, service, authorization, error handling, tests and the frontend Query hook — following project conventions. Use when a ticket needs a new or changed API route, or when the user invokes /api-endpoint.
argument-hint: <METHOD /api/v1/path> [purpose]
---

# Add an API endpoint

Endpoint: `$ARGUMENTS`

## 1. Check the contract

- Find the route in the PRD API outline (`docs/PRODUCT_REQUIREMENTS.md`). If it's missing or different, say so and update the outline in the same change.
- Decide: who may call it (Admin / Manager / Member / Viewer / non-member), the request and response shape, the status codes.

## 2. Backend (feature package `com.issuetracker.<feature>`)

| Piece | Rule |
| --- | --- |
| Request DTO | Java `record` with Bean Validation (`@NotBlank`, `@Size(max = 200)`, …) |
| Response DTO | `record`; never expose entities, password hashes or internal IDs that aren't needed |
| Mapper | static `from(entity)` on the response record, or a small mapper class |
| Controller | `@RestController`, `@RequestMapping("/api/v1/...")`, `@Valid @RequestBody`; returns DTOs; `201 Created` + `Location` header for creates, `204` for deletes |
| Service | `@Transactional`; business rules here; loads the project by key and checks membership |
| Authorization | `@PreAuthorize("@projectAccess.canWrite(#projectKey, authentication)")` style; Viewer → `403`, non-member → `404` so project existence isn't leaked |
| Errors | throw domain exceptions; the global `@RestControllerAdvice` maps them to `ProblemDetail`: `400` validation, `403`, `404`, `409` (stale `@Version` or unique conflict) |
| Activity | ticket field changes write an `ActivityLog` row in the same transaction |

## 3. Backend tests

- Service unit test (Mockito): happy path plus each business rule.
- API test (`@SpringBootTest` + MockMvc + Testcontainers):
  - happy path → status and body;
  - validation error → `400` with field errors;
  - Viewer → `403`; non-member → `404`; unauthenticated → `401`;
  - conflict case, if one applies → `409`.

## 4. Frontend

- Types in `frontend/src/api/types.ts`, matching the response DTO.
- Hook in `frontend/src/api/<feature>.ts`: `useQuery` for reads (key `['<feature>', …params]`), `useMutation` for writes, which invalidates or updates the right query keys.
- Board moves: optimistic update in `onMutate`, roll back in `onError`.
- Test the hook or the component that uses it with mocked HTTP (MSW).

## 5. Verify

Run `/verify backend` (and `frontend` if touched). Report the endpoint, its status codes, and the tests added.
