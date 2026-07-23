# Admin goods filter semantic repair (2026-07-17)

## Scope

Repair only the admin-goods filter compile mismatch and synchronize the real query parameters. Do not alter database schema, execute SQL, control services, use Git, or expand product features.

| Phase | Status | Evidence |
| --- | --- | --- |
| 1. Contract and query-path audit | completed | DTO, service, controller, BaseMapper path, API wrapper, view, API contract, and handoff inspected. |
| 2. Focused frontend/query synchronization | in_progress | Preserve the distinct shop and goods category filters; prohibit an unscoped goods-category selection. |
| 3. Compile, regression, package, and frontend build | pending | Run the four requested commands from the required paths and record actual results. |

## Findings

- `AdminGoodsServiceImpl.page` already uses `shopCategoryId` only to resolve `qh_shop.category_id` into shop IDs, and uses `goodsCategoryId` only against `qh_goods.category_id`.
- `AdminGoodsView.vue` still initializes and submits the obsolete `categoryId` query key, so it cannot drive either server-side filter correctly.
- `GoodsMapper` is a MyBatis-Plus `BaseMapper`; no XML query condition exists for this endpoint.
