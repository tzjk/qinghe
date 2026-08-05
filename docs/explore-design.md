# 探店与附近店铺设计

## 数据与一致性

- `qh_explore_post`、`qh_explore_like`、`qh_explore_comment` 是独立的新领域表；旧 `qh_blog` 相关表不复用。
- 点赞由 `qh_explore_like(post_id,user_id)` 联合唯一键和 MySQL 写入结果确定。只有插入或删除真实记录后才更新 `like_count`，计数不会小于零。
- Redis `qh:zset:explore:hot` 是热门排序加速层：提交后增加或减少分数；Redis 异常不回滚数据库事务，列表回退 `like_count DESC, created_at DESC`。缓存丢失可调用 `ExploreHotService.rebuild()`，从已展示内容的 MySQL 计数重建该单一 Key。

## 附近店铺与隐私

- `qh_shop.longitude/latitude` 使用 `DECIMAL(9,6)` 与 `DECIMAL(8,6)`，不使用数据库 `DOUBLE`。成功提交的店铺新增、坐标更新或停用会精确增删 `qh:geo:shop` 成员。
- 附近查询仅读取店铺状态为启用且有坐标的记录，Redis GEO 按距离升序返回；Redis 不可用时以 MySQL 候选集和服务端 Haversine 距离降级。
- 浏览器位置仅保留在页面内用于当前请求，不向后端保存用户实时坐标。用户拒绝授权时不从宿舍房间号推断；当前校区/楼栋没有中心坐标配置时提示手动选择校区并输入中心坐标。

## 审核

管理员使用状态 `PUBLISHED/DISABLED` 审核内容与评论，保留历史，不将删除作为默认审核手段；用户自行删除内容时写 `DELETED` 状态。
