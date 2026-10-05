---
name: create-tickets
description: Turn user stories from docs/PRODUCT_REQUIREMENTS.md (or a feature description) into GitHub issues using the ticket template, with labels, milestones and dependencies. Use when the user says "create tickets", "make issues from the PRD", "break this into tickets", or invokes /create-tickets.
argument-hint: "[story IDs | area | milestone | free-text feature]"
---

# Create tickets

Input: `$ARGUMENTS` — story IDs (`TKT-1 TKT-2`), an area (`board`), a milestone number (`3`), free text describing a new feature, or nothing (all PRD stories).

## 1. Collect stories

- From the PRD: the "Functional requirements" section. Each `**ID**` bullet is one story; its nested bullets are acceptance criteria.
- Free text: write stories in the same "As a … I can …" form with testable acceptance criteria, and new IDs in the right area prefix.
- Skip stories that already have an issue: `gh issue list --state all --limit 200 --json number,title` and match `[ID]` in titles.

## 2. Shape each ticket

Use `.github/ISSUE_TEMPLATE/ticket.md`:
- Title: `[<ID>] <short imperative title>`.
- Acceptance criteria: the PRD's criteria, plus ones implied by the PRD's non-functional requirements and rules (authorization for forbidden roles, validation errors, `409` on stale version).
- Technical notes: endpoints from the PRD API outline, entities, migration needed or not, UI pieces.
- Depends on: e.g. tickets need projects; the board needs tickets; everything needs AUTH-2.
- A story bigger than ~1–2 days → split it (e.g. backend API and board UI) with `a`/`b` suffixes: `[BRD-2a]`.

Add one setup ticket per milestone where it's needed: `[SETUP-1] Scaffold backend, frontend, docker-compose and CI`.

## 3. Show before creating

Show a table: ID · title · labels · milestone · depends on. Wait for the user to confirm. Creating issues is visible to the whole repo.

## 4. Create

Labels (create the missing ones once):
```bash
gh label create "area:auth" --color 5319e7 --force   # also area:projects, area:tickets, area:board, area:assignees, area:comments, area:setup
gh label create "type:story" --color 0e8a16 --force
gh label create "layer:backend" --color 1d76db --force
gh label create "layer:frontend" --color fbca04 --force
```

Milestones from the PRD's milestone list, if they're missing:
```bash
gh api repos/{owner}/{repo}/milestones -f title="M1 Foundations"
```

Create the issues in dependency order so "Depends on" can use real numbers:
```bash
gh issue create --title "[TKT-1] Create a ticket" --body-file <tmp> --label "type:story,area:tickets,layer:backend" --milestone "M3 Tickets"
```
Write each body to a temp file in the scratchpad directory.

## 5. Report

A table of the created issue numbers and links, plus anything skipped and why.
