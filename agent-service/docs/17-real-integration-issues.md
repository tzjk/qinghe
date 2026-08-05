# Phase 4 真实联调问题与阻塞

| 编号 | 现象 | 影响 | 安全处理 | 状态 |
|---|---|---|---|---|
| P4-001 | `GET /actuator/health` 返回 HTTP 404 | 不能使用 Actuator 作健康证明 | 已按规则改用已确认公共 GET；6 个公共接口均成功 | 已缓解 |
| P4-002 | 原 `.venv` 基于 Anaconda，Click 导入时其 `ctypes` 的 `_ctypes` DLL 被拒绝访问 | 原环境不能启动 Uvicorn | 保留原环境，使用独立 CPython 创建 Agent 内 `.venv-clean`；子进程清理 Conda 标记/PATH 条目；未改系统变量或 Anaconda | 已解决 |
| P4-003 | 尚未提供普通用户 Bearer Token | 未验证当前用户个人成功路径、过期 Token、他人订单和个人空数据的真实结果 | 未读取 LocalStorage、MySQL 或 Redis；个人用例只会在临时 Header/`QINGHE_TEST_USER_TOKEN` 下运行 | 等待用户 |
| P4-004 | 真实 `real_backend` 命令尚未在离线回归通过后收到再次确认 | 阶段 B--D 的真实 pytest 证据不存在 | 测试有标记和环境双门禁，默认不访问 8090 | 已解决：2 passed、1 skipped |
| P4-005 | 新 PowerShell 辅助脚本在 Windows PowerShell 中以无 BOM UTF-8 解析异常；停止其父 shell 不会自动停止 Uvicorn 子进程 | 首次启动与清理未达验收 | 脚本仅转为 UTF-8 BOM；清理时验证 8100 所有者为本轮 Uvicorn 后精确停止并确认端口释放 | 已解决 |
| P4-006 | 无法在不停止用户维护的 Spring Boot 的前提下制造真实 8090 暂时不可用；真实空数据/业务失败依赖现有公共数据状态 | 这些特定真实故障状态没有伪造的 Socket 证据 | 已有离线契约覆盖；本轮不停止 Spring、不写数据、不改后端 URL | 保留风险 |

## 未验证项

- 后端暂时不可用、确定为空的公共数据和业务失败码的真实 Socket 结果（不能通过停止 Spring 或写入数据制造）。
- 无效/过期真实 Token、他人订单、订单不存在、优惠券/订单为空和无入住记录的真实结果（需要普通用户 Token 或特定自然数据状态）。
- 普通用户 `get_my_profile`、`get_my_student_profile`、`get_my_dorm_info`、`get_my_coupon_wallet`、`get_my_recent_orders`、`get_my_order_detail`、`get_my_addresses` 的真实结果。

没有为绕过这些阻塞而访问写接口、管理员接口、MySQL、Redis、OSS、浏览器存储或真实模型。
