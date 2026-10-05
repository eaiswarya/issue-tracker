---
name: review-ticket
description: Review the implementation of a ticket — a PR, branch or local changes — against the issue's acceptance criteria, the PRD and project conventions, and report findings by severity. Use when the user says "review ticket #N", "review PR #N", "review my branch", or invokes /review-ticket.
argument-hint: "[pr-number | branch | issue-number] [--post]"
---

# Review a ticket

Input: `$ARGUMENTS`. `--post` means post the review on GitHub; otherwise report in chat only.

## 1. Find the change and its ticket

| Input | Diff | Ticket |
| --- | --- | --- |
| PR number | `gh pr diff <n>`, `gh pr view <n> --json title,body,headRefName,files` | `Closes #X` in the PR body |
| Branch | `git diff origin/main...<branch>` | issue number in the branch name |
| Issue number | find its PR: `gh pr list --search "<n>" --state open` | the issue |
| Nothing | `git diff origin/main...HEAD` + uncommitted changes | issue number in the current branch name |

Load the issue (`gh issue view <X>`) and the PRD story it names. No linked ticket → still review, but say so as a finding.

Read every changed file in full, not just the diff hunks, plus the code the diff calls.

## 2. Check, in this order

**A. Acceptance criteria** — make a table: criterion · met? · evidence (file:line) · test that covers it. A criterion with no test is not met.

**B. Correctness**
- Edge cases: empty, null, max length, duplicates, concurrent edits (`@Version`), deleted or archived records.
- Transactions: multi-write operations in one `@Transactional` service method; ticket numbering uses a row lock.
- Frontend: loading, error and empty states; optimistic board updates roll back on failure.

**C. Security**
- Every new endpoint has an authorization rule and a test for a forbidden role (Viewer, non-member).
- Access is checked against the project in the path; no IDOR via IDs in the body.
- DTO validation on input; entities never serialised directly; no password hashes or tokens in responses or logs.
- User Markdown rendered only through the sanitiser.

**D. Data**
- Schema changes in a new Flyway migration; applied migrations untouched; entities match the migration; indexes on new foreign keys and filter columns.

**E. Conventions** (`CLAUDE.md`) — package by feature, thin controllers, ProblemDetail errors, Query hooks in `src/api/`, branch and commit naming.

**F. Tests** — meaningful assertions (not just "no exception"), Testcontainers for repository and API tests, no sleeps or order dependence.

**G. Scope** — changes unrelated to the ticket; anything that belongs in another ticket.

## 3. Verify claims

If the branch is checked out locally, run `/verify`. Otherwise read the PR's CI status: `gh pr checks <n>`.
Only report a finding you have confirmed in the code. Mark guesses as questions.

## 4. Report

```
Verdict: Approve | Approve with nits | Changes requested

Acceptance criteria: <n>/<total> met
| Criterion | Status | Evidence | Test |

Blocking
1. <file>:<line> — <problem> → <concrete fix>

Should fix
...

Nits
...

Questions
...
```

Blocking = a bug, a security gap, an unmet criterion, or failing checks.

## 5. Post (only with --post or if the user asks)

- Each blocking or should-fix finding with a line → inline comment via `gh api repos/{owner}/{repo}/pulls/<n>/comments` (needs `commit_id`, `path`, `line`, `side: RIGHT`).
- Summary → `gh pr review <n> --comment --body-file <file>`, or `--request-changes` if there are blocking findings. Use `--approve` only if the user says so; you can't approve your own PR.
