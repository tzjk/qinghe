# Student profile and dorm QR check-in test closure (2026-07-17)

## Scope

Diagnose the Maven `testCompile` failure, complete the specified student-side tests, run the full regression and both builds, then update the requested delivery documents. No SQL execution, schema changes, service control, Git, real OSS, or unrelated dorm features.

| Phase | Status | Completion evidence |
| --- | --- | --- |
| 1. Context, source, and build-environment audit | completed | Required documents and module sources reviewed; Q: mapping, toolchain, POM, classpath, and complete test log captured. |
| 2. Focused review and test completion | completed | Public profile VO excludes `currentFlag`; student-dorm integration coverage includes safe profile fields, QR privacy/occupied state, uniqueness conversion, transaction rollback, concurrency, logs, and dorm-me. |
| 3. Full regression and builds | completed | `mvn -Dmaven.repo.local=Q:\.m2 clean test` passed 56/0/0/0; backend package and frontend production build passed. |
| 4. Documentation and manual acceptance closure | completed | Required API, contract, page, plan, progress, findings, dorm plan, and handoff records updated; marker data and login-token cleanup assertions passed. |

## Findings log

- Historical notes show student-dorm code exists but the latest Maven test attempt stopped in `testCompile` before any tests ran. Packaging with `-Dmaven.test.skip=true` is not test evidence.
- The current PowerShell session has no Q: drive. A temporary same-process mapping must be verified before using `Q:\backend`; it will be removed in the command cleanup path.
- `Q:\` was verified to map to `C:\Users\28402\Desktop\ruanzhu\workplace\qinghe-life-service`; with `-Dmaven.repo.local=Q:\.m2`, Maven executed 44 tests with 0 failures and 0 errors.
- The former `AdminGoodsQuery.getCategoryId()` compilation blocker has been resolved in the current workspace. This closure resumes from the student-dorm source and current test baseline only.

## Error log

| Attempt | Result | Next action |
| --- | --- | --- |
| Append to legacy `task_plan.md` using garbled-console context | Patch context did not match; no workspace content changed. | Keep the legacy plan intact, use this dedicated plan for the active milestone, and update the required legacy document during the closure phase with an exact ASCII anchor. |
| First Q: invocation | `subst Q:` was incorrectly used as a query and aborted before Maven. | Replaced it with the valid zero-argument `subst` mapping listing; the temporary mapping was removed after the later run. |
| Fresh Q: repository | The sandbox denied downloading the Spring Boot parent POM, so Maven could not read the project. | Re-ran the exact command with approved network access; Maven compiled and executed 44 tests. |
| Latest-source C: control run | Main compilation fails on the unrelated missing `getCategoryId()` accessor before test compilation. | Do not select a goods filter field without user approval; stop this milestone and report the blocker. |
| Closure child session | The temporary Q: mapping was not inherited, so Maven ran outside `backend` and executed no tests. | Create, use, and remove the Q: mapping inside the single Maven process. |
| Closure Maven invocation | Sandbox cache write was denied, then the unquoted repository property was parsed as a Maven target. | Use approved cache access and quote `-Dmaven.repo.local=Q:\.m2`; the next full run reached tests. |
| First closure test run | The new unique-index test asserted a localized response phrase even though the interface correctly returned code 409. | Assert the stable public contract: 409 and no leaked database constraint name; rerun the complete suite. |

## Final evidence

- Full Maven suite: 56 tests, 0 failures, 0 errors, 0 skipped. `StudentDormIntegrationTest` contributes 9 tests.
- Backend package: successful with `-DskipTests`, including test compilation; JAR generated at `backend/target/qinghe-life-backend-1.0.0.jar`.
- Frontend build: successful with 1718 transformed modules. The existing Rollup PURE-comment and chunk-size messages are non-blocking warnings.
- Test cleanup: the student-dorm test uses only `STUDENT_DORM_TEST_` users, student numbers, buildings, asset sets, check-ins, logs, and login-token keys; `@AfterEach` removes them precisely and `@AfterAll` verifies no marker rows or `qh:login:token:STUDENT_DORM_TEST_*` keys remain.
