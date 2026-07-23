# Git and GitHub workflow

## Branch model

- `main`: stable, demo-ready, releasable code. Do not develop on it long term.
- `develop`: daily integration branch.
- `feature/*`, `fix/*`, `hotfix/*`, `refactor/*`, `test/*`, `docs/*`, `chore/*`, `release/*`: short-lived purpose branches.

Examples: `feature/order-cancel`, `feature/dorm-agent`, `fix/dorm-qr-scan`, `refactor/dorm-mybatis-plus`, `test/order-concurrency`, and `docs/deployment-guide`.

Recommended flow: `feature/* -> develop -> release/* -> main`. A hotfix starts from `main`, is merged back to both `main` and `develop`, and receives a release note when verified.

## Commits and pull requests

Use Conventional Commits: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `build`, `ci`, `perf`, `style`, and `revert`.

Examples:

```text
feat(order): implement atomic stock deduction
fix(dorm): reject occupied bed reassignment
test(dorm): cover transfer rollback
docs(deploy): add local startup guide
refactor(asset): replace duplicate CRUD with MyBatis-Plus
```

Each commit has one clear purpose. Include relevant tests and documentation on the same feature branch where practical. Do not fabricate timestamps or split work merely to increase commit count.

PRs use the repository template and must state purpose, business/DB/Redis/API/security impact, validation, manual acceptance, rollback, and screenshots or demo notes where useful. Before requesting review, rebase is not required: update safely with `git fetch origin` and `git pull --ff-only` on the branch when appropriate.

Never commit real passwords, tokens, keys, local database backups, generated large files, `target`, `dist`, `node_modules`, or virtual environments. Do not force-push `main` or a shared branch, rewrite history to hide mistakes, or submit generated large files without review.

## GitHub remote and collaboration

Recommended repository name: `qinghe-life-service`. Prefer SSH:

```bash
git remote add origin git@github.com:<username>/qinghe-life-service.git
```

HTTPS is acceptable, but never place a PAT in a command, repository file, or documentation. After explicit authorization and a successful security review, the first-push sequence is:

```bash
git push -u origin main
git push -u origin develop
git push origin --tags
```

Daily work uses `git fetch origin`, `git pull --ff-only`, and `git push origin <branch>`. Confirm `git remote -v`, `git status`, and `git log --oneline -5` immediately before a first push. Local documentation must not assume GitHub authentication exists.

Configure GitHub branch protection for `main` and `develop`: block force pushes, require the CI check and an up-to-date branch before merge, use one documented merge policy (Squash Merge is recommended), and automatically delete merged feature branches. Do not commit GitHub tokens, SSH private keys, or credential files.

## Required PR review checklist

Review the change purpose, business impact, database and manual-SQL impact, Redis keys, API contract, permissions/security, test and build results, manual acceptance, rollback, and screenshots/demo evidence. The PR template is the authoritative checklist.
