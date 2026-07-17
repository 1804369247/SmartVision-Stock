# SmartVision Stock v1.0.0 发布说明

智能仓储管理系统（3D 仓库）首个完整实现版本。包含后端能力补齐、并发安全加固、工程化优化与前端联调修复。

## 后端（Spring Boot + JWT + WebSocket + 库存预占）

### 新增能力
- **库存预占（Reserved Stock）机制**：审核时逻辑锁定、定时过期自动释放，可用量 = 库存数量 − 活动预占量。
- **Redis**：分布式 JWT 黑名单 + `@Cacheable` 缓存（`!redis` profile 下自动排除，回退内存实现）。
- **Docker**：`Dockerfile` + `docker-compose.yml` 一键部署（含 nginx 托管前端）。
- **MinIO**：文件存储抽象接口 + MinIO 实现，本地磁盘兜底。
- **WebSocket 鉴权**：子协议 / Header 传 token（保留查询参数回退，便于调试）。

### 工程化 / 正确性优化
- **出库确认加悲观写锁**：扣减前对所有涉及的 `goodsInstanceId`（`distinct` + `sorted` 固定加锁顺序防死锁）逐个 `findByIdForUpdate` 加锁，预检与扣减复用同一批已加锁实体，杜绝并发确认拆分预占时的超卖 / 丢失更新。
- **`/instances` 接口分页化**：返回 `Page<GoodsInstance>`，支持 `page`（默认 0）/ `size`（默认 20）参数，按 id 倒序。
- **关闭默认 SQL 日志**：`show-sql:false`、`org.hibernate.SQL:WARN`，可用环境变量 `JPA_SHOW_SQL` / `LOG_LEVEL_SQL` 按需开启。
- **移除 `GoodsInstance` 无用 `version` 列**：去 `@Version` 后该字段已无意义，并发安全改由悲观锁保证。
- **WebSocket CORS 去掉 `*` 兜底**：`app.cors.origins` 为空时 warn 并拒绝所有跨域握手，不再回退通配符。

### 测试
- 新增并发预占 / 超卖 `@SpringBootTest` 集成测试（`H2`，4 用例）：单线程超卖拒绝、预占-消耗-释放生命周期、10 线程并发预占不超卖、8 线程并发确认不出现负库存。
- 全量测试 **104 例，0 失败，BUILD SUCCESS**。

## 前端（Vue 3 + Vite + Three.js）
- 修复 `OrderManagement.vue` 对 `/instances` 分页响应的消费逻辑（原代码把整个 `Page` 对象赋给下拉数组，导致 `v-for` 遍历错误）；下拉查询传 `size=1000` 避免被默认 20 条截断。

## 安全 / 部署
- `.gitignore` 已排除 `.workbuddy/`、密钥文件（`backend/.jwt_secret`）、运行时 `uploads/` 目录与构建 hack 目录（`backend/fakejdk/`），并清理垃圾文件。
- 所有密钥均走环境变量占位（`application-*.yml` 使用 `${...}`），无硬编码泄露；`.env` 含真实密钥且已被忽略。

## 升级 / 启动说明
- 后端构建：`mvn -o package`，运行 `java -jar target/smartvision-stock-1.0.0.jar --server.port=8080`。
- 前端：`node_modules/vite/bin/vite.js --port 8081`（开发）或 `vite build` + nginx 托管（生产，见 `docker-compose.yml`）。
- 数据库：MySQL `smartvision_stock`（root/123456），`ddl-auto:update` 首次启动自动建表；`DataInitializer` 初始化 `admin/admin123`、`operator/operator123`、主仓库、默认供应商。
- 默认 CORS 允许来源含 `localhost:8081/8082/8083/3000/5173` 及 ngrok 域名，生产请在 `application-prod.yml` 中改为真实域名。

## 提交信息
- 主提交：`3dea7c8 feat: 3D 仓储管理系统完整实现与库存预占/并发/工程化优化`
