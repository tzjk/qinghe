# 真实联调受控计划

默认 `mvn test` 不运行真实社交测试。仅在人工提供隔离环境，并同时设置 `QINGHE_REAL_SOCIAL_TESTS=true` 和 `QINGHE_TEST_REDIS_NAMESPACE=qh:test:social:{runId}:` 时，Maven 的 `real-social-tests` profile 才激活；保留测试入口会先校验 namespace 格式。

禁止使用 FLUSHDB、FLUSHALL、`KEYS *` 批量清理或操作开发用户关系。真实测试只使用人工准备的测试用户和本次 runId Key；结束时输出候选 Key 供人工确认，无法确认归属的数据不自动删除。SQL 迁移始终由用户手工执行。
