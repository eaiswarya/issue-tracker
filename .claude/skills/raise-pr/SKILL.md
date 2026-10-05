---
name: raise-pr
description: Verify the current branch, push it and open a GitHub pull request filled from the repo's PR template and the linked ticket. Use when the user says "raise a PR", "open a PR", "push and create PR", or invokes /raise-pr.
argument-hint: "[--draft] [base-branch]"
---

# Raise a PR

Input: `$ARGUMENTS`. `--draft` opens a draft PR. Base branch defaults to `main`.

## 1. Pre-flight

```bash
git status --porcelain
git branch --show-current
git fetch origin
git log --oneline origin/main..HEAD
```

Stop and tell the user if:
- the current branch is `main`;
- there are no commits ahead of `origin/main`;
- there are uncommitted changes (ask: commit them, stash them, or leave them out);
- a PR already exists for the branch: `gh pr view --json url,state` succeeds → show the link and ask whether to update it instead.

Warn if the branch name does not match `<type>/<issue#>-<slug>`.

## 2. Bring it up to date

If `origin/main` has moved ahead, rebase: `git rebase origin/main`. On conflicts, stop and show the files; do not resolve them by guessing.

## 3. Verify

Run `/verify`. If anything fails, stop and report. Only continue with failing checks if the user explicitly says so, and then say so in the PR body under "How to test".

## 4. Gather content

- Issue number from the branch name → `gh issue view <n> --json title,body`.
- Commits: `git log --format='%s%n%b' origin/main..HEAD`.
- Changed files: `git diff --stat origin/main...HEAD`.
- Read `.github/pull_request_template.md`.

## 5. Write the body

Fill every section of the template:
- **Ticket:** `Closes #<n>` and the PRD story ID.
- **What changed:** a 1–3 sentence summary and bullets of the main changes. Write about what changed and why, not a list of files.
- **Acceptance criteria:** each criterion from the issue, ticked only if a test covers it, with the test name.
- **How to test:** concrete steps a reviewer can run.
- **Screenshots:** delete this section for backend-only changes; for UI changes, leave a placeholder and tell the user to add images.
- **Checklist:** tick only what is true.

End the body with the attribution line set for this session, if there is one.

PR title = Conventional Commit style without the issue number: `feat(board): move cards between columns`.

## 6. Push and open

```bash
git push -u origin HEAD
gh pr create --base <base> --title "<title>" --body-file <tmpfile> [--draft]
```

Write the body to a temp file in the scratchpad directory, not the repo. Add labels that match the issue: `--label <label>`.

## 7. Report

Give the PR URL and any unticked criteria or checklist items. Offer `/review-ticket <pr#>` for a self-review.
