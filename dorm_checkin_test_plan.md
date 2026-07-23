# Admin Dorm Checkin Test Closure Plan

## Scope

Complete automated verification and delivery closure for administrator checkin query, checkout, and transfer only. Do not change schema, run manual SQL, control services, use Git, or add scope outside this module.

| Phase | Status | Evidence |
|---|---|---|
| Baseline review and targeted code fixes | in_progress | Admin interceptor and `AdminContext` verified; transaction write-count checks being added. |
| Specialist integration tests | pending | Query, checkout, transfer, concurrency, rollback, logs, student linkage, cleanup. |
| Full regression and builds | pending | Maven test, Maven package, frontend build with real outputs. |
| Residue and documentation closure | pending | Exact-prefix data and keys are zero; requested docs updated. |

## Verified constraints

- `active_flag=1` is current and `NULL` is history.
- Bed status remains directory state; asset sets use `AVAILABLE`/`OCCUPIED`.
- The repository-local `backend` is the verification target because `Q:\backend` is outside the allowed workspace.
