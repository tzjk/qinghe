# 购物车图片修复发现

## 初始核查

- `git branch --show-current` 输出 `develop`。
- `git status -sb` 显示已有未跟踪目录 `.planning/2026-07-28-soft-copyright-materials/`，属于本轮开始前的工作区内容；按用户规则不切换分支、不拉取、不重置、不清理、不暂存，也不修改该目录。
- `HEAD` 与 `origin/develop` 均为 `62f1c36`（初始核查时）。

## 待查

- 静态源代码显示商品主数据字段命名为 `coverImage`，购物车条目对外字段为 `goodsImage`；需要继续核实字段映射、赋值和前端绑定。
- 已发现 `CartIntegrationTest` 包含 `goodsImage` JSONPath 断言，需读取测试和实现以确认当前分支的完整实际行为。
- 管理端新增/编辑商品保存后通过独立图片上传接口更新 `coverImage`；需要核查该字段对应的实际表列和购物车读取路径。
- 前端商品卡使用 `goods.coverImage || goodsPlaceholder`，需要对照购物车页面是否错误读取同名字段。

## 已确认的数据链路

- 商品实体 `Goods` 映射表 `qh_goods`，Java 属性为 `coverImage`；MyBatis-Plus 默认下划线映射对应实际列应为 `cover_image`。管理员创建/编辑接口不接收图片字段，图片由 `POST /api/admin/goods/{id}/image` 成功上传后写入 `Goods.coverImage`。
- `CartServiceImpl.summary()` 用一次 `selectBatchIds` 查询全部购物车商品并构建 `goodsById`，没有逐购物车项查询商品；`CartItemVO.from` 将 `goods.getCoverImage()` 赋给 JSON 字段 `goodsImage`。
- 商品或店铺不存在的购物车项维持既有跳过规则；图片为空时 `goodsImage` 保持 null，不伪造 URL。新增、数量和选中更新返回时使用单个商品查询，未改变列表路径的批量查询策略。
- 前端 `CartView.vue` 使用 `item.coverImage` 作为 `<el-image>` 条件和来源，但 `GET /api/cart` 返回的是 `goodsImage`；商品卡和后台页面正确使用 `coverImage` 是因为它们消费商品接口，不是购物车条目接口。
- 因此图片数据链路原始问题为：后端与前端的购物车字段名称不一致，而非商品图片保存、OSS URL 拼接或 MyBatis-Plus 映射失败。

## 修复与测试覆盖

- `CartView.vue` 现消费 `item.goodsImage`；有值时由 `el-image` 以 `cover` 显示。空值直接使用现有 `goods-placeholder.svg`；URL 加载失败后以同一占位图回退，并为图片和占位提供商品名称 alt 文本。
- 扩展 `CartIntegrationTest`：同一购物车汇总含两条不同图片 URL 与一条 null 图片 URL，逐商品 ID 断言 `goodsImage`、名称、价格、库存、数量和选中状态。既有下架/缺失商品业务断言保持不变。
- 前端没有测试脚本或单元测试依赖；将以生产构建和源代码路径核对验证模板、字段绑定与回退逻辑。
- 未执行任何数据库 SQL。根据项目已记录的真实结构，人工可用 `SELECT id, shop_id, name, cover_image FROM qh_goods WHERE id IN (...);` 核对实际测试商品主图；本轮未读取当前库记录，不能将历史记录当作当前数据证明。

## 最终审计

- 前端生产构建在受控重试中成功：Vite 5.4.21 转换 1745 个模块并成功输出生产产物；仅有既有第三方 PURE 注释和包体积警告。
- `git diff --check` 通过。Git 差异仅涉及 `frontend/src/views/CartView.vue` 和 `backend/src/test/java/com/qinghe/life/CartIntegrationTest.java`；未见敏感信息、数据库 SQL、构建产物、环境文件或无关业务代码。
- 未创建分支、未切换分支、未拉取、未提交、未推送。当前工作区还保留开始前已有的 `.planning/2026-07-28-soft-copyright-materials/`，本轮未触碰。
