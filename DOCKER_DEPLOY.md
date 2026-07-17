# SmartVision Stock 容器化部署（Docker）

本文档说明如何使用 Docker Compose 一键启动完整的智能仓储管理系统：
**MySQL + Redis + MinIO + 后端(Spring Boot) + 前端(Vue/Nginx)**。

## 一、目录结构

```
SmartVision Stock/
├── docker-compose.yml        # 编排文件（一键启动 5 个服务）
├── .env                      # 部署环境变量（密码、密钥等）
├── backend/
│   ├── Dockerfile            # 后端镜像（多阶段：maven 构建 → jre 运行）
│   └── .dockerignore
└── frontend/
    ├── Dockerfile            # 前端镜像（node 构建 → nginx 运行）
    └── nginx.conf            # 反向代理 /api 与 /topic(WebSocket)
```

## 二、快速开始

1. 安装 [Docker](https://www.docker.com/) 与 Docker Compose（Docker Desktop 已内置）。
2. 修改 `.env`，**至少把 `JWT_SECRET` 改成随机长字符串**：
   ```bash
   JWT_SECRET=$(openssl rand -hex 32)
   ```
3. 在项目根目录执行：
   ```bash
   docker compose up -d --build
   ```
4. 等待所有服务 `healthy`（首次构建较慢）：
   ```bash
   docker compose ps
   ```
5. 浏览器访问：
   - 前端界面：<http://localhost:8081>
   - 后端 API：<http://localhost:8080>
   - MinIO 控制台：<http://localhost:9001>（账号见 `.env`）

## 三、各服务说明

| 服务 | 镜像 | 端口 | 说明 |
|------|------|------|------|
| mysql | mysql:8.0 | 3306 | 业务数据库，数据持久化到 `mysql-data` 卷 |
| redis | redis:7-alpine | 6379 | 分布式 Token 黑名单 + `@Cacheable` 缓存（redis profile） |
| minio | minio/minio | 9000/9001 | 对象存储，图片存于 `smartvision-stock` 桶（minio profile） |
| backend | 本地构建 | 8080 | Spring Boot，默认激活 `redis,minio` 两个 profile |
| frontend | 本地构建 | 8081 | Nginx 托管前端静态资源并反向代理 API/WebSocket |

> 后端通过 `SPRING_PROFILES_ACTIVE=redis,minio` 激活 Redis 与 MinIO 实现；
> 若去掉该变量，则回退为内存黑名单 + 本地磁盘存储（适合本地开发 demo）。

## 四、常用命令

```bash
# 查看日志
docker compose logs -f backend

# 停止并删除容器（保留数据卷）
docker compose down

# 停止并删除容器及数据卷（清空所有数据，慎用）
docker compose down -v

# 重新构建后端
docker compose up -d --build backend
```

## 五、生产环境建议

1. **务必修改** `.env` 中的 `JWT_SECRET`、`DB_PASSWORD`、`MINIO_*` 为强口令。
2. 为前端 Nginx 配置 HTTPS（可叠加 `certbot` 或反向代理）。
3. 将 `CORS_ORIGINS` 收紧为真实前端域名，避免 `*`。
4. 数据库、MinIO 建议改用托管服务或定期备份数据卷。
5. 可通过 `JAVA_OPTS` 环境变量调整后端 JVM 参数（如 `-Xmx1g`）。

## 六、本地开发（不使用 Docker）

前后端可独立运行，详见 `README.md`：

- 后端：`mvn package -DskipTests` 后用 `java -DJWT_SECRET=... -jar target/*.jar` 启动。
- 前端：`npm install && npm run dev`（Vite 开发服务器，端口 8081，自动代理 `/api` 与 `/topic`）。
