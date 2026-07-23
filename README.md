# 青禾校园生活服务系统 V1.0

本地演示项目，采用 Spring Boot 2.7.18、MyBatis-Plus、MySQL、Redis 和 Vue 3 技术栈。

## 当前进度

M1 已建立前后端项目骨架、公共组件、SQL、实体和 Mapper；用户业务接口将在后续里程碑实现。

## 目录

- `backend`：Java 后端工程与初始化 SQL。
- `frontend`：Vue 3 前端工程与页面骨架。
- `docs`：数据库、接口和页面设计。
- `deploy`：本地部署说明。

## 本地运行准备

后端使用 8090 端口，前端使用 5174 端口。数据库与 Redis 的连接参数在后端 `application.yml` 中通过环境变量占位表达式配置；请在本地环境设置相应变量后再启动服务。

初始化 SQL 位于 `backend/src/main/resources/sql/qinghe_life.sql`，仅供人工确认后导入，本项目不会自动执行 SQL。

## 约束

- 本项目只保存在本地，使用 `progress.md`、`findings.md` 和文件清单记录修改。
- 不接入真实支付、短信、云存储、消息队列或搜索引擎。
- M1 不启动后端，不连接 MySQL 或 Redis。
