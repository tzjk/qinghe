# Contributing

1. Start from `develop` and create one purpose-named branch.
2. Keep the change scoped; do not modify production secrets, database state, or unrelated generated files.
3. Run the relevant Maven and/or frontend build checks, then record factual results in the PR.
4. Update API, deployment, or migration documentation when the change affects it. SQL migrations are reviewed and executed manually; never auto-import them.
5. Open a PR to `develop` (or a documented release/hotfix target) using the PR template.

Before committing, inspect `git status --short` and the staged diff. Never add `.env` files, private keys, passwords, tokens, `target`, `dist`, `node_modules`, virtual environments, dumps, logs, or unreviewed generated large files.

For a personal project, self-review is still required: verify the same checklist before merging. Do not bypass branch protection, force-push a shared branch, or rewrite shared history.
