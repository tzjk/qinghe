# 本地部署指南

## 环境

- JDK 8、Maven 3.8+、Node.js 18+、MySQL 8、Redis 6+。
- MySQL 建库与 SQL 导入均由维护人员手工审核执行；应用不会自动执行迁移 SQL。

## 凭据配置

在启动终端设置 `MYSQL_PASSWORD` 与 `REDIS_PASSWORD`，如启用头像功能再设置 `OSS_ACCESS_KEY_ID`、`OSS_ACCESS_KEY_SECRET`。不得把真实值写入代码、`.env`、文档、截图或日志。后端配置不再提供密码默认值。

## 启动与验证

1. 在 `backend` 执行 `mvn "-Dmaven.repo.local=C:/Users/28402/.m2/repository" spring-boot:run`。
2. 在 `frontend` 执行 `D:/develop/NodeJS/npm.cmd run dev`。
3. 后端地址为 `http://localhost:8090`，前端地址为 `http://localhost:5174`。
4. 发布前执行：`mvn "-Dmaven.repo.local=C:/Users/28402/.m2/repository" test`、`mvn "-Dmaven.repo.local=C:/Users/28402/.m2/repository" -DskipTests package`、`D:/develop/NodeJS/npm.cmd run build`。

生成物为 `backend/target/qinghe-life-backend-1.0.0.jar` 与 `frontend/dist`，均不得提交到版本库。
