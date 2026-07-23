# Release process

## Three levels of history

1. A **commit** records a small, reviewable daily increment.
2. An **annotated tag** records a verified stable milestone.
3. A **GitHub Release** publishes a demonstrable version with its release notes.

Use semantic versions. The intended milestone map is only a planning guide: `v0.1.0` framework, `v0.2.0` authentication/shops/goods, `v0.3.0` dorm assets and QR check-in, `v0.4.0` normal orders, `v0.5.0` student-status changes and batch dorm management, and `v1.0.0` first complete deployable version. Create a tag only when its stated scope and verification evidence are real.

## Release checklist

1. Create `release/<version>` from `develop`; update `CHANGELOG.md` and release documentation.
2. Run applicable backend and frontend build checks, plus local integration tests that need MySQL/Redis. Record failures honestly.
3. Merge the verified release through a PR into `main`, then sync `develop`.
4. Create an annotated tag, for example:

```bash
git tag -a v0.3.0 -m "Dorm asset and check-in milestone"
git push origin v0.3.0
```

5. Create the GitHub Release from that immutable tag with scope, migration notes, validation, known limits, and rollback notes.

Never move a pushed formal tag casually or tag unfinished work. Use `Added`, `Changed`, `Fixed`, `Security`, `Deprecated`, and `Removed` in the changelog.
