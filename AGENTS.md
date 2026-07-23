\# 青禾校园生活服务系统开发规则



\## 工作目录



当前项目目录名称为 qinghe-life-service。



只允许读取、创建、修改和删除当前 qinghe-life-service 仓库中的文件。



不得读取、复制或修改当前仓库之外的文件，包括上层目录中的软著模板、软著申请文件、黑马点评项目、苍穹外卖项目和其他本地项目。



所有代码、SQL、文档、测试、静态资源和部署文件都必须保存在当前仓库内。



\## 外部环境限制



不得停止、启动或重启 Windows 服务。



不得修改系统环境变量。



不得删除已有 MySQL 数据库。



不得执行 DROP DATABASE。



不得执行 Redis 的 FLUSHALL 或 FLUSHDB。



不得自动导入 SQL。



不得自动修改现有 MySQL、Redis、DataGrip 或 IDEA 配置。



不得执行 npm 全局安装。



本项目默认在本地开发；只有在用户明确书面授权后，才可以使用 Git 和 GitHub 进行版本管理与远程备份。



未经用户明确书面授权，不得执行 git init、git status、git add、git commit、git push 或其他 Git 命令。获得授权后，Git 操作仅限于当前 qinghe-life-service 项目及用户指定的 GitHub 仓库。



缺少 Git 仓库不是错误；仅在用户要求版本管理或远程推送时创建或配置仓库。



使用文件清单、progress.md、findings.md 和 PROJECT_PLAN.md 记录项目修改情况。



\## 固定技术配置



项目中文名称：青禾校园生活服务系统 V1.0



英文工程名：qinghe-life-service



后端包名：com.qinghe.life



Java：8



Spring Boot：2.7.18



MyBatis-Plus：3.5.x



MySQL：8



后端端口：8090



前端端口：5174



数据库名称：qinghe\_life



Redis 数据库编号：2



Redis 键统一使用 qh: 前缀



MySQL 与 Redis 的具体连接参数以 docs/PROJECT_SPEC.md 和 docs/database-design.md 为准；不得将数据库或 Redis 密码写入 README.md、API.md、前端页面或软著文案。



请求头统一使用：



Authorization: Bearer <token>



后端使用 Maven。



前端使用 Vue 3、Vite、Element Plus、Pinia、Vue Router 和 Axios。



\## 工作方式



优先使用以下 Skills：



\- planning-with-files

\- context-compression

\- context-degradation



每次任务开始前读取：



\- AGENTS.md

\- docs/PROJECT\_SPEC.md

\- PROJECT\_PLAN.md

\- progress.md

\- findings.md



必须先规划，再编码。



一次只执行一个里程碑。



未经用户确认，不得提前执行下一个里程碑。



每个里程碑结束后必须：



1\. 更新 progress.md。

2\. 更新 findings.md。

3\. 列出新增和修改文件。

4\. 运行对应测试、编译或构建。

5\. 如实记录成功和失败结果。

6\. 停止并等待用户审核。



验证失败时不得声称已经完成。



\## 项目原创要求



不得复制黑马点评或苍穹外卖源码。



不得出现以下原项目标识：



\- com.sky

\- com.hmdp

\- 黑马

\- 苍穹外卖

\- 黑马点评



允许学习登录、缓存、购物车、订单、优惠券和后台管理的通用实现方式，但包名、类名、表名、接口、页面、字段和文案必须重新设计。



\## 项目边界



不接入真实支付。



不接入真实短信服务。



不接入云存储。



不实现消息队列、分布式事务、搜索引擎或高并发秒杀。



开发验证码固定为 123456，并允许保存到 Redis。



支付采用模拟支付状态。



SQL 文件只生成，不自动执行。



\## 文案限制



禁止词限制只作用于：



\- frontend/src/assets/copyright-text.md

\- 前端用户可见的项目介绍文案


不作用于 Java 代码、技术文档、依赖名称和必要的开发说明。

