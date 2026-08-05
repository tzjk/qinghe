# Coupon duplicate-claim display and feedback

## Goal

Expose existing normal-coupon idempotency with an explicit claim result, a batch-mapped list claim state, and disabled/reliable client feedback. Retain the MySQL transaction, conditional stock decrement, unique constraint, and scope boundaries.

| Phase | Status | Acceptance evidence |
|---|---|---|
| 1. Audit and contract | completed | Existing retry returns only `UserCouponVO`; the available list has no claim fields and the UI always permits claiming. Identity comes only from `UserContext`. |
| 2. Backend state and focused tests | in_progress | Add explicit claim status/result; batch-map claim state without N+1; cover retry, concurrency, all statuses, and refreshed-list state. |
| 3. Frontend state and feedback | pending | Disable claimed/in-flight cards, avoid duplicate requests, and show backend retry status then refresh. |
| 4. Verification, records, and Git | blocked | Requested tests/builds passed, checks and the single commit completed; normal push was attempted once but GitHub HTTPS could not connect through local proxy `127.0.0.1`. |

## Hard boundaries

- Do not accept `userId`; retain the MySQL transaction, `available_stock > 0` decrement, and `uk_qh_user_coupon(user_id,coupon_id)`.
- List records expose `claimed`, `userCouponId`, and `userCouponStatus`; bulk-map the current page. `AVAILABLE`, `LOCKED`, `USED`, and `EXPIRED` all remain claimed.
- Do not run dormitory, academic-status, or asset tests. Do not execute SQL or start/stop services.
