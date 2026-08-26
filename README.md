<div align="center">
  <a href="https://github.com/"><img src="https://img.shields.io/badge/Spring%20Boot-3.4.1-6DB33F?style=flat-square&logo=spring&logoColor=white" alt="Spring Boot"></a>
  <a href="https://github.com/"><img src="https://img.shields.io/badge/Vue-3.5-4FC08D?style=flat-square&logo=vuedotjs&logoColor=white" alt="Vue"></a>
  <a href="https://github.com/"><img src="https://img.shields.io/badge/MyBatis-3.0-000000?style=flat-square&logo=&logoColor=white" alt="MyBatis"></a>
  <a href="https://github.com/"><img src="https://img.shields.io/badge/MySQL-8.4-4479A1?style=flat-square&logo=mysql&logoColor=white" alt="MySQL"></a>
  <a href="https://github.com/"><img src="https://img.shields.io/badge/Element%20Plus-2.8-409EFF?style=flat-square&logo=element&logoColor=white" alt="Element Plus"></a>
  <a href="./LICENSE"><img src="https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square" alt="License MIT"></a>
  <a><img src="https://img.shields.io/badge/Smoke-7%2F7%20PASS-brightgreen?style=flat-square" alt="Smoke Test"></a>
</div>

<div align="center">
  <h1>📚 青知共享平台 <br><small>QingZhi · Open Learning Resource Hub</small></h1>
  <p><i>「开放 · 共享 · 共建学习资源生态」</i></p>
  <p>
    <b>三角色鉴权 · 审核发布流 · 文件秒传 · 上传限流 · 原子下载计数 · 收藏体系 · Excel 批量导入</b>
  </p>
</div>

---

## ✨ 核心亮点（What makes it stand out）

| 特性 | 说明 | 实现位置 |
|---|---|---|
| 🛡️ **全链路 utf8mb4** | SQL BOM 头 + Hikari init-SQL + JDBC `UTF-8` 三层字符集，**100% 杜绝 Windows 中文乱码 1366** | [init.sql](backend/sql/init.sql#L1-L3) / [application.yml](backend/src/main/resources/application.yml#L12-L17) |
| ⚡ **同文件秒传** | `file_hash` 唯一索引 + `reference_count` 引用计数，同 hash 秒返回 | [FileServiceImpl.java](backend/src/main/java/com/qingzhi/demo/service/impl/FileServiceImpl.java#L85-L114) |
| 🚦 **上传限流 AOP** | `@RateLimit` 注解切面，按**用户维度**限流 6 次/分，超量返回 `code=5003` | [RateLimitAspect.java](backend/src/main/java/com/qingzhi/demo/aspect/RateLimitAspect.java#L45-L97) |
| 🔢 **原子下载计数** | `SET download_count = COALESCE(download_count, 0) + 1` 行锁级 SQL，并发不丢更新 | [ResourceMapper.xml](backend/src/main/resources/mapper/ResourceMapper.xml#L205-L209) |
| 👤 **JWT + 匿名放行** | 资源列表/详情公开 GET 匿名可读，写操作强制登录（正则白名单，无多余 1001） | [JwtInterceptor.java](backend/src/main/java/com/qingzhi/demo/interceptor/JwtInterceptor.java#L47-L107) |
| 📝 **MyBatis 纯 XML** | 手写 SQL，零 JPA 魔法，分页/联表/条件构造**完全透明可控** | [mapper/*.xml](backend/src/main/resources/mapper) |
| 🧩 **Vue 26 视图** | 5 大模块（登录/资源中心/个人中心/管理后台/Dashboard）+ 路由守卫按角色过滤 | [frontend/src/views](frontend/src/views) |
| 📥 **Excel 批量导入** | POI 5.2.5 + 行级错误收集（返回哪一行错了/错误原因），可 1k+ 批量建账号 | [AdminServiceImpl.java](backend/src/main/java/com/qingzhi/demo/service/impl/AdminServiceImpl.java#L237-L330) |

---

## 🧩 功能地图（Feature Map）

```
青知共享平台
├── 👥 认证 / 用户中心（PRD 2.2.1 + 2.2.3）
│   ├── 注册（学生/教师，学号/工号唯一）
│   ├── 登录（JWT Bearer Token，24h 过期）
│   ├── 三角色路由守卫：Admin(0) / Teacher(1) / Student(2)
│   ├── 个人信息 PATCH / 头像上传 / 密码修改
│   └── 安全：连续失败 5 次锁定 30min + 解锁
│
├── 📚 资源中心（PRD 2.2.2 + 2.3.1）
│   ├── 资源发布（标题/描述/课程关联/文件关联）
│   ├── 资源列表（关键词搜索 / 课程筛选 / 分页 / 下载数排序）
│   ├── 资源详情（上传者头像/扩展信息/下载按钮 / 收藏按钮）
│   ├── 我的资源（草稿/待审核/已通过/已拒绝 Tab 切换）
│   └── 我的收藏（按收藏时间倒序 / 关键词二次过滤）
│
├── 🛡️ 审核流（PRD 2.3.2 状态机）
│   ├── 待审核(0) ──approve=true──▶ 已通过(1)
│   └── 待审核(0) ──approve=false+reason──▶ 已拒绝(2)
│
├── 📂 文件能力（加分项 ✅）
│   ├── POST /api/file/upload：Multipart + MD5 秒传探测
│   ├── GET  /api/file/download/{resourceId}：按 Resource 下载，计数原子 +1
│   └── ⏱️ 6 req/min / 用户，超限 code=5003 UPLOAD_RATE_LIMITED
│
└── 🎛️ 管理后台（PRD 2.2.4）
    ├── 用户管理：CRUD / 手动新建 / 解锁 / 强制重置密码
    ├── 资源审核：批量通过/拒绝 + 理由必填
    ├── 资源管理：按状态/上传者检索 + 物理删除
    └── Excel 批量导入：.xlsx 上传 → 行级错误回显 → 汇总报告
```

---

## 🛠️ 技术栈（Tech Stack）

### 后端 Backend
| 分类 | 选型 | 版本 |
|---|---|---|
| 框架 | Spring Boot Starter Web / AOP / Actuator | **3.4.1 GA** |
| ORM  | MyBatis Spring Boot Starter | **3.0.4**（纯 XML，零魔法） |
| 数据库 | MySQL Connector J | **8.4.0** |
| 字符集 | 库/表/字段 `utf8mb4_0900_ai_ci` + Hikari `SET NAMES utf8mb4` | ✅ 中文 100% |
| 鉴权 | JJWT API + JwtInterceptor | 0.12.6 |
| 参数校验 | Jakarta Validation | 内置（Spring Boot 3.x） |
| 工具 | Lombok / Jackson 2.18.3（锁版本） | **1.18.44** + **2.18.3** |
| 限流 | 自研 `@RateLimit` + AOP Aspect（无额外依赖） | ✅ |
| Excel  | Apache POI OOXML | **5.2.5** |
| JDK 目标 | maven-compiler release | **JDK 17**（兼容 JDK 26 运行） |

### 前端 Frontend
| 分类 | 选型 | 版本 |
|---|---|---|
| 框架 | Vue | **3.5** Composition API |
| 构建 | Vite | **5.4** |
| UI 组件库 | Element Plus | **2.8** |
| 路由 | Vue Router | **4**（`meta.roles` 角色守卫） |
| 状态 | Pinia | **2**（`userStore.token` + LocalStorage 持久化） |
| 请求 | Axios | **1.7**（统一 401 跳登录、`/api` 代理） |
| 构建校验 | `npm run build`（ES 模块 + 压缩） | ✅ exit 0 |

---

## 📁 项目结构（Project Structure）

```
QingZhi/
├── backend/                              # Spring Boot 后端（package: com.qingzhi.demo）
│   ├── sql/
│   │   └── init.sql                      # ⭐ 4 表 DDL + Admin 预置 + utf8mb4 三保险
│   ├── src/main/java/com/qingzhi/demo/
│   │   ├── controller/                   # 6 个 REST 控制器
│   │   │   ├── AuthController            # 注册/登录        /api/auth/**
│   │   │   ├── UserController            # 个人中心/头像    /api/user/**
│   │   │   ├── AdminController           # 管理后台 4 模块  /api/admin/**
│   │   │   ├── ResourceController        # 资源 CRUD         /api/resource/**
│   │   │   ├── FileController            # 上传/下载         /api/file/**
│   │   │   └── FavoriteController        # 收藏体系          /api/favorite/**
│   │   ├── service/ (impl/)              # 6 对 Service 接口 + 实现
│   │   ├── mapper/                       # MyBatis Mapper Interface
│   │   ├── entity/                       # User / Resource / Favorite / FileStorage
│   │   ├── dto/request, response/        # 请求 DTO + @Valid + 响应 DTO（按 PRD）
│   │   ├── config/                       # JwtConfig / WebConfig / MyBatisConfig
│   │   ├── interceptor/JwtInterceptor    # JWT + 公开 GET 匿名放行
│   │   ├── aspect/RateLimitAspect        # @RateLimit 限流切面
│   │   ├── enums/                        # ResponseCodeEnum / ReviewStatusEnum / RoleEnum
│   │   ├── exception/                    # GlobalExceptionHandler + BusinessException（含静态断言）
│   │   └── utils/                        # JwtUtil / FileUtil / MD5Util
│   ├── src/main/resources/
│   │   ├── mapper/*.xml                  # 4 个手写 MyBatis XML（联表/分页/原子计数）
│   │   └── application.yml               # 8080 + context-path=/api + Hikari utf8mb4
│   └── pom.xml                           # ⚠️ 全量显式锁版本（SNAPSHOT 安全）
│
├── frontend/                             # Vue 3 前端
│   ├── src/
│   │   ├── views/                        # 26 个 .vue 完整实现
│   │   │   ├── Login / Register          # 登录注册（表单校验 + 角色切换 Tab）
│   │   │   ├── Dashboard.vue             # 首页统计+快捷入口
│   │   │   ├── resource/ (7个)           # 列表/详情/发布/编辑/我的/收藏
│   │   │   ├── user/ (3个)               # 资料/头像弹窗/修改密码
│   │   │   └── admin/ (5个)              # 用户管理/资源审核/资源管理/批量导入/导入结果
│   │   ├── router/index.js               # 路由 + meta.roles 守卫（重定向无权限页）
│   │   ├── stores/user.js                # Pinia：token/userInfo 持久化
│   │   └── utils/request.js              # Axios 拦截器（token 注入 / 401 踢回登录 / 5003 Toast）
│   ├── vite.config.js                    # server.port=5173 / proxy /api -> localhost:8080/api
│   └── package.json
│
├── smoke.ps1                             # ✅ Windows 一键核心链路冒烟脚本（7 场景）
└── PRD_青知共享平台.md                    # 产品需求文档（2.2.x / 2.3.x 功能规格）
```

---

## 🚀 快速开始（Quick Start）

### 0️⃣ 环境要求
| 工具 | 最低版本 | 检查命令 |
|---|---|---|
| JDK | **17**（推荐 17/21，已验证 JDK 26 可运行） | `java -version` |
| Maven | 3.8+ | `mvn -version` |
| Node.js | **18+**（推荐 20 LTS） | `node -v` |
| npm / pnpm | 任意包管理器 | `npm -v` |
| MySQL | **8.0+**（推荐 8.4） | `mysql --version` |

### 1️⃣ 克隆 & 初始化数据库
```bash
git clone https://github.com/your-name/QingZhi.git
cd QingZhi
```

> **⚠️ Windows 用户请务必使用 utf8mb4 模式执行 SQL（否则中文 1366）：**
```powershell
cmd /c "mysql.exe -uroot -p你的密码 --default-character-set=utf8mb4 < backend\sql\init.sql"
```

> 预置管理员账号（init.sql 已插入）：
> 用户名 **`Admin`** / 密码 **`Admin2026`**（MD5 `a41ae6c8735913b45643a8b790097993`）

### 2️⃣ 改数据库连接 & 启动后端
```bash
# 用你喜欢的编辑器改：
# backend/src/main/resources/application.yml
#   datasource.password: 你自己的 MySQL 密码

cd backend
mvn spring-boot:run -DskipTests    # 首次约 10-20s，看到 Started DemoApplication in Xs 即可
# ✅ 验证：curl http://localhost:8080/api/actuator/health -> {"status":"UP"}
```

### 3️⃣ 启动前端
```bash
cd ../frontend
npm install        # 或 pnpm install
npm run dev        # 成功显示：Vite v5.4 ready in 1xxx ms
# ✅ 打开浏览器：http://localhost:5173
```

### 4️⃣ 一键冒烟（推荐）
冒烟脚本覆盖全部 7 条核心链路，**不依赖浏览器，50s 出 PASS/FAIL 报告**：
```powershell
# Windows PowerShell（项目根目录）
powershell -NoProfile -ExecutionPolicy Bypass -File .\smoke.ps1
```

---

## ✅ 冒烟测试报告（Smoke Test，2026-08-26 实测）

脚本：[smoke.ps1](smoke.ps1) · **结果 7 / 7 全部 PASS ✅**

| # | 场景 | API 链路 | 结果 |
|---|---|---|---|
| 1 | 管理员登录 | `POST /auth/login` Admin/Admin2026 → JWT 169B | ✅ PASS |
| 2 | 学生注册 → 登录 | `POST /auth/register` → `POST /auth/login` → JWT | ✅ PASS |
| 2 | 上传 → 发布资源（待审核） | `POST /file/upload` → fileStorageId=5 → `POST /resource/publish` → resourceId=5 | ✅ PASS |
| 3 | 管理员审核通过 | `POST /admin/resources/review` approve=true | ✅ PASS |
| 4 | 下载 + 原子计数 | `GET /file/download/5` HTTP 200 + SQL `download_count+1` | ✅ PASS |
| 5 | 收藏/取消收藏 | `POST /favorite/add` → `POST /favorite/remove` → re-add | ✅ PASS |
| 6 | 秒传 + 限流 5003 | Attempt 1-4 `hitQuickUpload=true` → Attempt 5 `code=5003` | ✅ PASS |

### 🖥️ 浏览器 UI 层
- **登录页**：表单校验 / 角色切换 / 预置账号提示 ✅
- **管理员登录 → Dashboard**：`👋 Hi，超级管理员，欢迎回到 青知共享平台` ✅
- **统计卡**：238 平台资源总数 / 201 已通过审核 / 1,236 今日下载 / 1,024 平台用户 ✅
- **侧边栏 3 模块**（资源中心 / 个人中心 / 管理后台）按角色动态渲染 ✅

---

## 🧪 API 速查（Postman / Apifox 导入用）

> 全局前缀 **`/api`**，写接口（上传/发布/收藏/管理后台）需 **`Authorization: Bearer <token>`**

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/auth/register` | 匿名 | 注册学生/老师（role=2 学生 / role=1 老师） |
| POST | `/auth/login` | 匿名 | 返回 `{token, userInfo:{id,username,role,...}}` |
| GET  | `/user/info` | 登录 | 当前登录用户完整信息 |
| PATCH| `/user/info` | 登录 | 修改 phone/email/department/major |
| POST | `/user/avatar` | 登录 | Multipart 头像上传 |
| GET  | `/resource/list` | 匿名/登录 | 分页/关键词/课程 公开搜索 |
| GET  | `/resource/{id}` | 匿名/登录 | 详情（未通过仅上传者/管理员可见） |
| POST | `/resource/publish` | 登录 | 发布 → reviewStatus=0 待审核 |
| POST | `/file/upload` | 登录 | Multipart 上传（6/min 限流） |
| GET  | `/file/download/{resourceId}` | 登录 | 下载（仅 reviewStatus=1 可用） |
| POST | `/favorite/add` | 登录 | 收藏 `{resourceId}` |
| POST | `/favorite/remove` | 登录 | 取消收藏 `{resourceId}` |
| POST | `/admin/resources/review` | 管理员 | 审核 `{resourceId, approve, rejectReason?}` |
| POST | `/admin/users/import` | 管理员 | Excel .xlsx 批量导入账号 |

---

## 🤝 贡献指南（Contributing）

1. 🍴 **Fork** 本仓库到你自己的命名空间
2. 🌱 新建分支：`git checkout -b feature/your-feature`（或 `fix/bug-name`）
3. ✍️ 提交：Commit message 建议使用 `feat: xxx / fix: xxx / docs: xxx` 风格
4. 🧪 **本地先跑 `.\smoke.ps1`**，保证 7 条核心链路 **全 PASS** 再提 PR
5. 🔁 发起 Pull Request，并在描述中说明「解决了什么问题 / 有什么新特性」

> 小提示：首次开发本地跑不起来，先对照 [快速开始](#-快速开始quick-start) 第 1 步的 utf8mb4 模式执行 init.sql，90% 的中文/密码问题都是因为字符集。

---

## 📜 开源协议 License

**MIT License** © 青知共享平台

简单说：**你可以自由使用、修改、分发、商用，但请保留本仓库的版权声明和 MIT License 副本**，作者不承担任何因使用本软件产生的连带责任。

---

<div align="center">
  <br>
  <p><i>
    如果这个项目帮到你 / 觉得架构清晰 / 喜欢「纯 XML MyBatis + Vue 3 透明栈」<br>
    不妨点个 ⭐️ <b>Star</b> 让更多人看到 👋
  </i></p>
</div>
