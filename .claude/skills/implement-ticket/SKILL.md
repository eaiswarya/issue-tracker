---
name: implement-ticket
description: Implement a GitHub issue (ticket) end to end — read it, branch, plan, write tests first, implement, verify, commit. Use when the user says "implement ticket/issue #N", "work on #N", "pick up TKT-3", or invokes /implement-ticket.
argument-hint: <issue-number | PRD story ID>
---

# Implement a ticket

Input: `$ARGUMENTS` — an issue number (`12`, `#12`) or a PRD story ID (`TKT-3`).

## 1. Load the ticket

- Issue number → `gh issue view <n> --json number,title,body,labels,state,assignees,comments`.
- Story ID → `gh issue list --search "[<ID>] in:title" --state open --json number,title` and use the match. No match → stop and offer `/create-tickets`.
- Stop if the issue is closed, or is assigned to someone else (ask first).
- Read the matching story and domain-model sections in `docs/PRODUCT_REQUIREMENTS.md`.
- Check "Depends on": if a dependency issue is still open, tell the user and ask whether to continue.
- If acceptance criteria are missing or ambiguous, list the questions and ask before writing code.

## 2. Branch

```bash
git fetch origin && git status --porcelain
```
- Uncommitted changes → stop and ask.
- Create `<type>/<n>-<slug>` from `origin/main` (`feat` for stories, `fix` for bugs). Slug: 2–5 kebab-case words from the title.
- Assign yourself the issue only if the user agrees: `gh issue edit <n> --add-assignee @me`.

## 3. Plan (show it before coding)

Write a short plan and show it to the user:
- Acceptance criteria → the test that will prove each one.
- Files to add or change, backend and frontend.
- Migration needed? → use `/db-migration`. New endpoint? → follow `/api-endpoint`.
- Anything in the ticket that conflicts with the PRD or existing code.

For tickets that touch more than ~5 files, wait for the user to approve the plan. Smaller ones: proceed.

## 4. Build test-first, one criterion at a time

For each acceptance criterion:
1. Write the failing test. Run it and confirm it fails for the right reason.
2. Write the smallest code that passes.
3. Run the related tests again.
4. Commit: `<type>(<area>): <what> (#<n>)`.

Follow the conventions in `CLAUDE.md` (thin controllers, DTOs, `@PreAuthorize`, Flyway, TanStack Query hooks).

## 5. Verify

Run `/verify`. Fix failures. Never mark the ticket done with failing or skipped checks; report them.

## 6. Hand off

Report in a few lines:
- Branch name and commits.
- Each acceptance criterion → test that covers it.
- Anything deferred or questionable.

Then offer `/review-ticket` (self-review) and `/raise-pr`. Do not push or open a PR unless asked.
