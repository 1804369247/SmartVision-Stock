# SmartVision Stock - 智能仓储管理系统

基于3D可视化的智能仓储管理系统，支持实时库存监控、入库出库操作、库存预警等功能。

## 项目介绍

SmartVision Stock 是一个现代化的仓储管理系统，特色功能包括：
- 🎯 **3D可视化仓库**：使用 Three.js 实现仓库的3D展示，直观查看库位状态
- 📊 **实时数据更新**：WebSocket 推送库存变化，实时刷新界面
- 📋 **Excel导出**：支持库存、出入库记录等数据的Excel导出
- ⚠️ **库存预警**：低库存自动提醒，支持自定义预警阈值
- 🔍 **多维度查询**：按状态、属性、区域等多条件筛选

## 技术栈

### 前端
- **框架**：Vue 3 + Vite
- **UI组件**：Element Plus
- **3D渲染**：Three.js
- **状态管理**：Pinia
- **虚拟滚动**：vue-virtual-scroller

### 后端
- **框架**：Spring Boot 2.7.18
- **ORM**：Spring Data JPA + Hibernate
- **数据库**：MySQL 8.0
- **实时通信**：WebSocket
- **Excel导出**：Apache POI

## 项目结构

```
SmartVision Stock/
├── backend/                 # 后端Spring Boot项目
│   ├── src/main/java/      # Java源代码
│   ├── src/main/resources/ # 配置文件
│   └── pom.xml             # Maven配置
├── src/                    # 前端Vue项目
│   ├── api/               # API接口封装
│   ├── components/         # Vue组件
│   ├── composables/        # 组合式函数
│   ├── stores/            # Pinia状态管理
│   └── utils/             # 工具函数
├── dist/                   # 构建输出目录
├── package.json           # 前端依赖配置
└── vite.config.js        # Vite配置
```

## 快速开始

### 环境要求
- JDK 11+
- Node.js 16+
- MySQL 8.0+
- Maven 3.6+

### 后端启动

1. 创建数据库：
```sql
CREATE DATABASE smartvision_stock CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 配置环境变量（或直接在 `application.yml` 中修改）：
```bash
export DB_USERNAME=root
export DB_PASSWORD=your_password
```

3. 启动后端：
```bash
cd backend
mvn spring-boot:run
```

后端将在 `http://localhost:8080` 启动。

### 前端启动

1. 安装依赖：
```bash
npm install
```

2. 启动开发服务器：
```bash
npm run dev
```

前端将在 `http://localhost:5173` 启动。

### 生产构建

```bash
# 构建前端
npm run build

# 构建后端
cd backend
mvn clean package

# 运行jar包
java -jar target/smartvision-stock-0.0.1-SNAPSHOT.jar
```

## 配置说明

### 后端配置（application.yml）

关键配置项：
- `spring.datasource`：数据库连接配置
- `spring.jpa.hibernate.ddl-auto`：建议设置为 `update`（生产环境使用 `validate`）
- `server.port`：后端服务端口（默认8080）

### 前端配置（vite.config.js）

- 代理设置：将 `/api` 请求代理到后端服务
- 构建选项：输出目录为 `dist`

## API接口列表

### 基础数据管理
- `GET /api/goods` - 商品列表
- `POST /api/goods` - 新增商品
- `GET /api/locations` - 库位列表
- `POST /api/locations` - 新增库位
- `GET /api/suppliers` - 供应商列表

### 入库操作
- `POST /api/inbound` - 入库操作
- `GET /api/inbound-records` - 入库记录查询

### 出库操作
- `POST /api/outbound` - 出库操作
- `GET /api/outbound-records` - 出库记录查询

### 库存管理
- `GET /api/stock/query` - 库存查询
- `GET /api/stock/warning` - 预警库存列表
- `POST /api/stock/transfer` - 库位转移

### 导出接口
- `GET /api/export/inventory` - 导出库存Excel
- `GET /api/export/inout-records` - 导出出入库记录

## 功能模块

### 1. 基础数据管理
- 商品管理：维护商品信息（名称、分类、规格等）
- 库位管理：管理仓库库位（编号、区域、属性等）
- 供应商管理：维护供应商信息

### 2. 入库管理
- 入库操作：选择库位、商品、填写数量，完成入库
- 入库记录：查询历史入库记录

### 3. 出库管理
- 出库操作：从库位出库商品
- 出库记录：查询历史出库记录

### 4. 库存管理
- 库存查询：实时查看各库位库存状态
- 库存预警：低库存、近效期商品提醒
- 库位转移：商品在不同库位间转移

### 5. 报表中心
- 库存台账：完整库存明细报表
- 出入库流水：出入库操作历史
- 库位利用率：库位使用情况分析

## 已知问题修复

### ✅ 已修复
1. **数据库密码外部化**：从硬编码改为环境变量 `${DB_PASSWORD}`
2. **JPA配置优化**：`ddl-auto` 从 `create` 改为 `update`（避免重启时数据丢失）
3. **.gitignore配置**：添加正确的git忽略规则，防止 `node_modules` 被提交

### ⚠️ 待修复
1. **前端文件编码问题**：部分Vue/JS文件中文乱码，需要手动重建（无git历史可恢复）
2. **lodash版本号**：package.json中版本号错误（应为 `^4.17.21`）

## 贡献指南

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交 Pull Request

## 许可证

本项目为私有项目，未经授权不得使用、分发或修改。

## 联系方式

如有问题，请提交 Issue 或联系项目维护者。

---

**最后更新**：2026-06-04
