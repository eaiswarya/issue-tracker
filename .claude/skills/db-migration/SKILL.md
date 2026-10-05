---
name: db-migration
description: Add a PostgreSQL schema change as a new Flyway migration and keep JPA entities in sync. Use whenever a table, column, index or constraint must be added or changed, or when the user invokes /db-migration.
argument-hint: <what the change does>
---

# Database migration

Change: `$ARGUMENTS`

## Rules

- Location: `backend/src/main/resources/db/migration/`.
- Name: `V<next>__<snake_case_description>.sql`. `<next>` = highest existing version + 1. Check `origin/main` too, so you don't reuse a version another branch has already merged.
- **Never edit a migration that is on `main`.** Fix mistakes with a new migration.
- One logical change per file.
- The source of truth for names and types is the domain model table in `docs/PRODUCT_REQUIREMENTS.md`. If the change differs from it, update that table in the same PR.

## Conventions

- Tables: plural snake_case (`tickets`, `project_members`). Columns: snake_case.
- Primary keys: `id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY`.
- Timestamps: `created_at TIMESTAMPTZ NOT NULL DEFAULT now()`, `updated_at TIMESTAMPTZ NOT NULL DEFAULT now()`.
- Enums: `VARCHAR(32)` with a `CHECK (col IN (...))`, mapped with `@Enumerated(EnumType.STRING)`. No Postgres enum types; they're painful to alter.
- Foreign keys: named `fk_<table>_<column>`, with an explicit `ON DELETE`. Index every foreign-key column: `idx_<table>_<column>`.
- Unique rules: named constraints, e.g. `uq_tickets_project_number UNIQUE (project_id, number)`.
- Optimistic locking: `version BIGINT NOT NULL DEFAULT 0` where the entity has `@Version`.
- Soft delete: `deleted BOOLEAN NOT NULL DEFAULT false`, and index it with the common filter, e.g. `(project_id, status) WHERE deleted = false`.
- Adding a `NOT NULL` column to a table with data: add it nullable, backfill, then `SET NOT NULL`, all in the same migration.

## Steps

1. Write the migration.
2. Update or create the entity to match exactly (column names, nullability, lengths). `ddl-auto=validate` fails startup on any mismatch.
3. Add or adjust a repository test (Testcontainers) that covers the new constraint, e.g. a duplicate insert throws `DataIntegrityViolationException`.
4. Run `cd backend && ./mvnw -q test`. Flyway applies the migration to a fresh container.
5. Report the file name, what it does, and the entity changes.
