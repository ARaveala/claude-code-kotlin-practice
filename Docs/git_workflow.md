# Git Workflow — Plant App

## Branch Strategy
- `main` — stable, never worked on directly
- `feature/<short-name>` — one branch per feature/phase (e.g. `feature/area-list`,
  `feature/growzone-nesting`)
- Merge via PR even solo — open the PR on GitHub, read own diff before
  merging, squash-merge into main. The point is building the review habit,
  not process for its own sake.

## Sync Before Starting New Work
```bash
git checkout main
git pull origin main
git checkout -b feature/short-name
```

## Commit Message Convention
Format: `<type>: <short description in imperative form>`

Types:
- `feat:` — new feature or capability
- `fix:` — bug fix
- `refactor:` — code change that doesn't change behavior
- `test:` — adding or updating tests
- `docs:` — documentation only (README, CLAUDE.md, domain_model.md, etc.)
- `chore:` — tooling, dependencies, build config, gitignore, etc.

Examples:
```
feat: add GrowZone nesting validation
fix: correct scale calculation on manual resize entry
docs: update roadmap with Phase 3 sizing cap notes
```

## Commit-msg Hook (enforces the convention locally)
This lives in `.git/hooks/commit-msg`, it is NOT tracked by git by default,
so it needs to be set up once per clone, as working solo.

```bash
#!/bin/bash
# .git/hooks/commit-msg
commit_msg_file=$1
commit_msg=$(cat "$commit_msg_file")

pattern="^(feat|fix|refactor|test|docs|chore): .+"

if ! echo "$commit_msg" | grep -qE "$pattern"; then
    echo "ERROR: Commit message must start with feat:, fix:, refactor:, test:, docs:, or chore:"
    echo "Got: $commit_msg"
    exit 1
fi
```

Setup:
```bash
chmod +x .git/hooks/commit-msg
```

## Rebase Feature Branch Before PR
```bash
git checkout main
git pull origin main
git checkout feature/short-name
git rebase main
git push --force origin feature/short-name
```

## Squash Commits Before PR (optional, if a feature branch got messy)
```bash
git rebase -i HEAD~N   # N = number of commits on the branch
# in editor: keep first as `pick`, change rest to `squash`
```

## PR Checklist (self-review, solo project)
1. Read the full diff, not just the file list
2. Confirm it matches the current roadmap phase, nothing from a later
   phase snuck in
3. Confirm no secrets/API keys/personal paths are in the diff (see .gitignore)
4. Squash to one clean commit if the branch has a lot of "fix typo" noise
5. Merge into main, delete the feature branch