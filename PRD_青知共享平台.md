
# 青知共享平台 — 项目开发需求文档（PRD）

> 文档版本：v1.0  
> 整理日期：2026年8月    

---

## 一、项目背景与目标

### 1.1 项目背景

考试周期间，学生往往焦头烂额地到处零星搜集复习资料，效率低下且资料分散。秉持 **开放共享** 的理念，本项目旨在开发一个在线资料共享平台——**青知**，让教师和学生能够便捷地发布、查找与收藏学习资源，形成完整的资源共享生态。

### 1.2 项目目标

| 目标维度 | 描述 |
|---------|------|
| 核心目标 | 构建一套在线资源共享系统，支持教师/学生发布与查看资源、管理员进行系统管理 |
| 认证目标 | 实现完整的登录注册模块，支持批量导入与自行注册双通道 |
| 权限目标 | 基于角色（管理员/教师/学生）实现差异化权限控制 |
| 资源目标 | 支持资源发布、审核、收藏、分页查询、时间范围筛选等完整生命周期 |
| 质量目标 | 全局异常处理、日志跟踪、密码加密存储、JWT 鉴权 |

---

## 二、功能性需求

### 2.1 用户认证模块

#### 2.1.1 用户注册

| 注册方式 | 适用角色 | 账号来源 | 密码规则 | 备注 |
|---------|---------|---------|---------|------|
| 管理员固定账号 | 管理员 | 用户名：Admin | 密码：Admin2026 | 系统内置，不可自行注册 |
| 批量创建（Excel导入） | 教师/学生 | Excel 文件中含学号/工号 | 系统设定初始密码 | 管理员上传，系统自动创建 |
| 自行注册 | 学生 | 学号作为账号 | 自拟，>=8位，须含数字+字母 | 需校验密码格式 |
| 自行注册 | 教师 | 工号作为账号 | 自拟，>=8位，须含数字+字母 | 需校验密码格式 |

**双通道并行规则：**
- 系统校验学号/工号是否已存在（Excel 已导入则提示 **"账号已存在，请直接登录"**）
- 两种方式创建的账号统一管理，无差别对待

**密码校验规则：**

| 规则项 | 要求 |
|-------|------|
| 最小长度 | >= 8 位 |
| 字符组成 | 必须同时包含数字和字母 |
| 输入验证 | 前后端均需校验 |

#### 2.1.2 用户登录

| 项目 | 说明 |
|-----|------|
| 登录方式 | 账号 + 密码 |
| 鉴权机制 | Filter / Interceptor + JWT |
| 未登录访问 | 访问非登录注册接口时，返回"未登录"提示 |

#### 2.1.3 密码管理

| 项目 | 说明 |
|-----|------|
| 加密存储 | 使用加密算法（如 MD5）对密码进行加密后存储 |
| 密码重置 | 管理员可重置用户密码 |

---

### 2.2 角色权限模块

#### 2.2.1 角色定义与权限矩阵

| 功能点 | 管理员 | 教师 | 学生 |
|-------|--------|------|------|
| 用户信息增删改查（所有用户） | Y | - | - |
| 重置用户密码 | Y | - | - |
| 资源审核（通过/拒绝） | Y | - | - |
| 资源增删改查（所有资源） | Y | - | - |
| Excel 批量导入师生信息 | Y | - | - |
| 发布/删除/修改自己的资源 | - | Y | Y |
| 查看所有已通过资源 | Y | Y | Y |
| 收藏/取消收藏资源 | - | Y | Y |
| 查询/补充个人信息 | - | Y | Y |
| 修改自己的密码 | - | Y | Y |
| 查看自己发布的资源（分页） | - | Y | Y |
| 查看自己的收藏列表（分页） | - | Y | Y |

> Y = 允许，- = 不允许

#### 2.2.2 管理员权限详情

| 子模块 | 功能说明 |
|-------|---------|
| 用户管理 | 对所有用户信息进行增删改查，包括重置用户密码 |
| 资源管理 | 对发布的资源进行审核（通过后才能公开发布），同时可对资源进行增删改查 |
| 数据导入管理 | 上传符合规范的 Excel 批量导入师生信息；导入时需验证账号密码合法性（学号/工号格式、是否重复、必填字段完整性等） |

#### 2.2.3 普通用户权限详情

| 子模块 | 功能说明 |
|-------|---------|
| 个人信息管理 | 查询/补充个人信息、修改密码、查看自己发布的资源和收藏列表（分页）、收藏和取消收藏操作 |
| 资源操作 | 发布/删除/修改自己发布的资源；查看所有已发布资源；支持按发布时间范围查询（指定起止日期），查询结果按时间倒序分页展示；收藏感兴趣的资源 |

---

### 2.3 资源管理模块

#### 2.3.1 资源字段定义

| 字段 | 说明 |
|-----|------|
| 资源标题 | 文本，必填 |
| 资源描述 | 文本，选填 |
| 所属课程 | 文本，必填 |
| 上传文件 | 支持 PDF / PPT / Word / 图片等常见格式，单文件 <= 50MB |
| 发布者信息 | 关联用户（外键） |
| 发布时间 | 自动记录 |
| 审核状态 | 枚举：待审核 / 已通过 / 已拒绝 |
| 拒绝理由 | 审核拒绝时必填 |
| 文件存储位置 | 服务器本地目录或对象存储 |

#### 2.3.2 资源状态流转

```
+----------+  用户提交   +----------+  审核通过   +----------+
|  (无)    | ----------> |  待审核   | ----------> |  已通过   |
+----------+             +-----+----+             +-----+----+
                               |                        |
                               | 审核拒绝                | 用户修改
                               v                        v
                          +----------+             +----------+
                          |  已拒绝   |             |  待审核   |
                          +-----+----+             +----------+
                               |
                               | 用户修改后重新提交
                               v
                          +----------+
                          |  待审核   |
                          +----------+
```

| 状态流转规则 | 说明 |
|------------|------|
| 用户提交资源 | 状态 -> **待审核**，仅本人和管理员可见 |
| 管理员审核通过 | 状态 -> **已通过**，所有用户可见 |
| 管理员审核拒绝 | 状态 -> **已拒绝**，须填写拒绝理由；用户可修改后重新提交 |
| 用户修改已通过的资源 | 状态回退 -> **待审核**，需重新审核 |
| 用户删除资源 | 任意状态下均可删除 |

---

### 2.4 数据导入模块

#### 2.4.1 Excel 导入规范

| 项目 | 说明 |
|-----|------|
| 操作者 | 仅管理员 |
| 导入方式 | 上传符合规范的 Excel 文件 |
| 导入内容 | 教师/学生的信息（含账号与初始密码） |

**Excel 表头示例（学生）：**

| 列 | 字段 | 是否必填 | 说明 |
|----|------|---------|------|
| 1 | 学号 | 是 | 作为登录账号 |
| 2 | 姓名 | 是 |  |
| 3 | 手机号 | 否 |  |
| 4 | 邮箱 | 否 |  |
| 5 | 院系 | 是 |  |
| 6 | 专业 | 是 |  |

**Excel 表头示例（教师）：**

| 列 | 字段 | 是否必填 | 说明 |
|----|------|---------|------|
| 1 | 工号 | 是 | 作为登录账号 |
| 2 | 姓名 | 是 |  |
| 3 | 手机号 | 否 |  |
| 4 | 邮箱 | 否 |  |
| 5 | 院系 | 是 |  |

#### 2.4.2 导入合法性校验

| 校验项 | 说明 |
|-------|------|
| 学号/工号格式 | 是否符合编码规则 |
| 重复检查 | 学号/工号是否与已有账号重复 |
| 必填字段 | 是否完整填写 |
| 密码合法性 | 初始密码是否满足密码规则（>=8位，含数字+字母） |

---

## 三、非功能性需求

### 3.1 性能需求

| 需求项 | 说明 |
|-------|------|
| 分页查询 | 资源列表、收藏列表、个人资源列表均需支持分页 |
| 时间范围查询 | 资源支持按发布时间指定起止日期筛选，结果按时间倒序排列 |
| 文件大小限制 | 单文件上传不超过 50MB |

### 3.2 安全需求

| 需求项 | 说明 |
|-------|------|
| 密码加密存储 | 使用 MD5 或其他加密算法，禁止明文存储 |
| JWT 鉴权 | 通过 Filter/Interceptor + JWT 实现接口鉴权，未登录访问返回提示 |
| 登录防暴力破解（加分项） | 同一账号 5 分钟内连续失败 5 次，锁定 15 分钟 |
| 文件上传限流（加分项） | 同一用户每分钟最多上传 5 个文件 |

### 3.3 技术栈约束

#### 3.3.1 后端技术栈

| 类别 | 技术 | 说明 |
|-----|------|------|
| Web 框架 | Spring Boot | 快速开发与自动装配（版本 4.2.0-SNAPSHOT） |
| JDK 版本 | Java 26 | 项目使用 Java 26 进行编译运行 |
| 设计原则 | Spring IOC + AOP | 控制反转 & 面向切面编程 |
| 架构规范 | SpringMVC | Model-View-Controller 三层架构 |
| 数据库 | MySQL | 关系型数据库，数据库名：qingzhi_db |
| 持久层 | MyBatis | ORM 映射（mybatis-spring-boot-starter 3.0.4） |
| 项目管理 | Maven | 依赖管理与远程仓库 |
| 鉴权 | JWT + Interceptor | JSON Web Token 加密与权限校验（通过拦截器实现） |
| 日志 | SLF4J | 日志打印与跟踪 |
| 监控端点 | Spring Boot Actuator | 提供应用健康检查与监控端点 |
| Excel 处理 | 自实现工具类 | ExcelUtil 处理批量导入 |

#### 3.3.2 前端技术栈

| 类别 | 技术 | 说明 |
|-----|------|------|
| 构建工具 | Vite | 下一代前端构建工具，快速冷启动与热更新 |
| 核心框架 | Vue 3 | 渐进式 JavaScript 框架，使用组合式 API |
| 路由管理 | Vue Router | 单页面应用路由管理（history 模式） |
| 状态管理 | Pinia | Vue 官方推荐的状态管理库 |
| HTTP 请求 | Axios | 封装统一请求拦截器与响应拦截器（api/request.js） |
| 样式方案 | CSS3 + CSS 变量 | 通过 variables.css 管理全局样式变量 |
| 权限控制 | 自定义指令 + 工具函数 | directives/permission.js + utils/permission.js |
| 环境配置 | .env 文件 | .env.development / .env.production 区分环境 |

#### 3.3.3 前端项目结构

```
frontend/src/
├── api/                  # API 接口层（按模块拆分）
│   ├── request.js        # Axios 封装（统一拦截器）
│   ├── auth.js           # 认证相关接口（登录/注册）
│   ├── user.js           # 用户相关接口
│   ├── resource.js       # 资源相关接口
│   ├── favorite.js       # 收藏相关接口
│   └── admin.js          # 管理员相关接口
├── components/           # 通用组件
│   ├── common/           # 公共组件（Pagination、FileUpload、EmptyState、StatusTag）
│   ├── resource/         # 资源组件（ResourceCard、ResourceFilter）
│   └── admin/            # 管理组件（ImportResult）
├── directives/           # 自定义指令
│   └── permission.js     # 权限控制指令
├── layout/               # 全局布局
│   ├── Layout.vue        # 整体布局容器
│   ├── Header.vue        # 顶部导航
│   └── Sidebar.vue       # 侧边栏菜单
├── router/               # 路由配置
│   ├── index.js          # 路由实例创建
│   └── routes.js         # 路由表定义
├── stores/               # Pinia 状态管理
│   ├── index.js          # Pinia 入口
│   ├── userStore.js      # 用户状态（Token、用户信息）
│   └── appStore.js       # 应用全局状态
├── styles/               # 全局样式
│   ├── index.css         # 样式入口
│   └── variables.css     # CSS 变量定义
├── utils/                # 工具函数
│   ├── storage.js        # 本地存储封装
│   ├── format.js         # 格式化工具
│   ├── validate.js       # 校验工具
│   └── permission.js     # 权限判断工具
└── views/                # 页面视图
    ├── auth/             # 登录注册（Login.vue、Register.vue）
    ├── dashboard/        # 首页（Index.vue）
    ├── resource/         # 资源模块（列表、详情、发布、修改）
    ├── profile/          # 个人中心（信息、改密、我的资源、我的收藏）
    └── admin/            # 管理后台（用户管理、资源管理、资源审核、Excel导入）
```

### 3.4 代码规范

#### 3.4.1 命名规范

| 对象 | 规范 | 示例 |
|-----|------|------|
| 类名 | 大驼峰命名法 | UserController |
| 方法名/变量名 | 小驼峰命名法 | getUserById |
| 目录（文件夹） | 小写单个单词 | controller |
| 数据库表名/字段名 | 下划线命名法 | user_info |

#### 3.4.2 注释规范

| 位置 | 类型 | 说明 |
|-----|------|------|
| 实体类变量上方 | 段注释（/** */） | 描述字段含义 |
| 方法上方 | 段注释（/** */） | 描述方法功能、参数、返回值 |
| 业务逻辑代码间 | 行注释（//） | 必要处添加 |

#### 3.4.3 后端分包规范

```
com.qingzhi.demo/
├── DemoApplication.java      # Spring Boot 启动类
│
├── aspect/                   # AOP 切面包
│   └── RateLimitAspect.java  # 接口限流切面（文件上传频率限制）
│
├── common/                   # 公共基础包
│   ├── Constants.java        # 全局常量定义
│   ├── Result.java           # 统一响应封装（code + message + data）
│   └── PageResult.java       # 分页结果封装（分页列表 + 总数）
│
├── config/                   # 配置包
│   ├── JwtConfig.java        # JWT 配置（密钥、过期时间等）
│   ├── MyBatisConfig.java    # MyBatis 配置（分页插件、类型别名等）
│   └── WebConfig.java        # Web 配置（注册拦截器、跨域等）
│
├── controller/               # 控制层（接收请求，返回结果）
│   ├── AuthController.java   # 认证控制器（注册、登录）
│   ├── UserController.java   # 用户控制器（个人信息、改密、我的资源、我的收藏）
│   ├── ResourceController.java  # 资源控制器（发布、列表、详情、修改、删除）
│   ├── FavoriteController.java  # 收藏控制器（收藏、取消收藏、收藏列表）
│   ├── FileController.java   # 文件控制器（文件上传、下载）
│   └── AdminController.java  # 管理员控制器（用户管理、资源审核、Excel导入）
│
├── dto/                      # 数据传输对象（严格分包）
│   ├── request/              # 请求 DTO
│   │   ├── LoginRequest.java          # 登录请求
│   │   ├── RegisterRequest.java       # 注册请求
│   │   ├── PasswordResetRequest.java  # 密码重置请求
│   │   ├── ResourcePublishRequest.java  # 资源发布请求
│   │   ├── ResourceUpdateRequest.java   # 资源修改请求
│   │   └── ResourceQueryRequest.java    # 资源查询请求（含分页、时间范围）
│   └── response/             # 响应 DTO
│       ├── LoginResponse.java           # 登录响应（含 Token + 用户信息）
│       ├── UserInfoResponse.java        # 用户信息响应
│       ├── ResourceListResponse.java    # 资源列表响应
│       ├── ResourceDetailResponse.java  # 资源详情响应
│       └── FavoriteListResponse.java    # 收藏列表响应
│
├── entity/                   # 实体类（与数据库表一一映射）
│   ├── User.java             # 用户实体（对应 user 表）
│   ├── Resource.java         # 资源实体（对应 resource 表）
│   ├── Favorite.java         # 收藏实体（对应 favorite 表）
│   └── FileStorage.java      # 文件存储实体（对应 file_storage 表，秒传功能）
│
├── enums/                    # 枚举包
│   ├── RoleEnum.java         # 角色枚举（0-管理员 / 1-教师 / 2-学生）
│   ├── ResourceStatusEnum.java  # 资源审核状态枚举（0-待审核 / 1-已通过 / 2-已拒绝）
│   └── ResponseCodeEnum.java    # 响应码枚举（对应 PRD 5.3 错误码定义）
│
├── exception/                # 异常处理包
│   ├── BusinessException.java      # 业务异常（自定义业务逻辑异常）
│   └── GlobalExceptionHandler.java # 全局异常处理器（@ControllerAdvice）
│
├── interceptor/              # 拦截器包
│   └── JwtInterceptor.java   # JWT 鉴权拦截器（校验 Token 有效性、提取用户信息）
│
├── mapper/                   # 持久层（MyBatis Mapper 接口）
│   ├── UserMapper.java       # 用户 Mapper
│   ├── ResourceMapper.java   # 资源 Mapper
│   ├── FavoriteMapper.java   # 收藏 Mapper
│   └── FileStorageMapper.java  # 文件存储 Mapper
│
├── service/                  # 业务层接口
│   ├── AuthService.java      # 认证业务接口
│   ├── UserService.java      # 用户业务接口
│   ├── ResourceService.java  # 资源业务接口
│   ├── FavoriteService.java  # 收藏业务接口
│   ├── FileService.java      # 文件业务接口
│   ├── AdminService.java     # 管理员业务接口
│   └── impl/                 # 业务层实现
│       ├── AuthServiceImpl.java
│       ├── UserServiceImpl.java
│       ├── ResourceServiceImpl.java
│       ├── FavoriteServiceImpl.java
│       ├── FileServiceImpl.java
│       └── AdminServiceImpl.java
│
├── utils/                    # 工具类包
│   ├── JwtUtil.java          # JWT 工具（生成/解析 Token）
│   ├── PasswordUtil.java     # 密码工具（MD5 加密、格式校验）
│   ├── HashUtil.java         # 哈希工具（文件 MD5/SHA-256，秒传用）
│   ├── FileUtil.java         # 文件工具（上传路径处理、大小格式化）
│   └── ExcelUtil.java        # Excel 工具（批量导入解析、数据校验）
│
└── vo/                       # 视图对象
    ├── UserVO.java           # 用户视图对象（脱敏展示）
    └── ResourceVO.java       # 资源视图对象（含发布者信息、收藏状态等）
```

**包职责说明：**

| 包名 | 职责 |
|-----|------|
| aspect | AOP 切面实现（如限流、日志、事务等横切关注点） |
| common | 公共基础类（统一响应、分页结果、全局常量等） |
| config | 集中管理和配置应用程序的各种组件、属性和资源 |
| controller | 接收用户请求，调用业务层处理，返回结果 |
| dto/request | 接收前端请求参数的数据传输对象 |
| dto/response | 返回给前端的响应数据传输对象 |
| entity | 与数据库表一一映射的实体类 |
| enums | 枚举类型定义（角色、状态、错误码等） |
| exception | 自定义异常与全局异常处理器 |
| interceptor | Spring MVC 拦截器（鉴权、日志等） |
| mapper | 与数据库交互，实现持久化操作（CRUD） |
| service | 封装业务逻辑，实现核心功能和业务规则 |
| service/impl | Service 接口的具体实现类 |
| utils | 通用工具类和方法 |
| vo | 视图对象，用于前端视图层展示（可对 entity 做脱敏/组装） |

---

## 四、数据库设计要点

### 4.1 用户表（user）

| 字段名 | 类型 | 说明 | 约束 |
|-------|------|------|------|
| id | BIGINT | 主键 | PK, AUTO_INCREMENT |
| username | VARCHAR | 账号（学号/工号/Admin） | UNIQUE, NOT NULL |
| password | VARCHAR | 加密后的密码 | NOT NULL |
| name | VARCHAR | 姓名 | |
| phone | VARCHAR | 手机号 | |
| email | VARCHAR | 邮箱 | |
| department | VARCHAR | 院系 | |
| major | VARCHAR | 专业（仅学生） | 学生必填，教师可为空 |
| role | TINYINT / ENUM | 角色：0-管理员, 1-教师, 2-学生 | NOT NULL |
| status | TINYINT | 账号状态：0-正常, 1-锁定 | NOT NULL, DEFAULT 0 |
| login_fail_count | INT | 连续登录失败次数 | DEFAULT 0 |
| lock_time | DATETIME | 锁定开始时间 | 可为空 |
| create_time | DATETIME | 创建时间 | NOT NULL |
| update_time | DATETIME | 更新时间 | NOT NULL |

### 4.2 资源表（resource）

| 字段名 | 类型 | 说明 | 约束 |
|-------|------|------|------|
| id | BIGINT | 主键 | PK, AUTO_INCREMENT |
| title | VARCHAR | 资源标题 | NOT NULL |
| description | TEXT | 资源描述 | |
| course | VARCHAR | 所属课程 | NOT NULL |
| file_path | VARCHAR | 文件存储路径 | NOT NULL |
| file_name | VARCHAR | 原始文件名 | NOT NULL |
| file_size | BIGINT | 文件大小（字节） | |
| file_hash | VARCHAR | 文件哈希值（MD5/SHA-256，秒传用） | 加分项 |
| user_id | BIGINT | 发布者ID | FK -> user.id, NOT NULL |
| status | TINYINT / ENUM | 审核状态：0-待审核, 1-已通过, 2-已拒绝 | NOT NULL, DEFAULT 0 |
| reject_reason | VARCHAR | 拒绝理由 | 状态为已拒绝时必填 |
| create_time | DATETIME | 发布时间 | NOT NULL |
| update_time | DATETIME | 更新时间 | NOT NULL |

### 4.3 收藏表（favorite）

| 字段名 | 类型 | 说明 | 约束 |
|-------|------|------|------|
| id | BIGINT | 主键 | PK, AUTO_INCREMENT |
| user_id | BIGINT | 用户ID | FK -> user.id, NOT NULL |
| resource_id | BIGINT | 资源ID | FK -> resource.id, NOT NULL |
| create_time | DATETIME | 收藏时间 | NOT NULL |

> **唯一约束**：(user_id, resource_id) 联合唯一，防止重复收藏

### 4.4 文件存储表（file_storage）— 加分项（秒传）

| 字段名 | 类型 | 说明 | 约束 |
|-------|------|------|------|
| id | BIGINT | 主键 | PK, AUTO_INCREMENT |
| file_hash | VARCHAR | 文件哈希值（MD5/SHA-256） | UNIQUE, NOT NULL |
| file_path | VARCHAR | 实际存储路径 | NOT NULL |
| file_size | BIGINT | 文件大小 | |
| reference_count | INT | 引用计数 | DEFAULT 1 |
| create_time | DATETIME | 上传时间 | NOT NULL |

---

## 五、接口设计规范

### 5.1 统一响应格式

所有接口均返回 JSON 格式，统一结构如下：

```json
{
    "code": 1,
    "message": "Success!",
    "data": {
        "id": 123456,
        "stuNumber": "2024xxxxx",
        "name": "xxxxx"
    }
}
```

| 字段 | 类型 | 说明 |
|-----|------|------|
| code | Integer | 状态码：1 表示成功，其他表示失败（见错误码表） |
| message | String | 提示信息 |
| data | Object / Array / null | 业务数据 |

### 5.2 请求方式规范

| 场景 | 请求方式 | 说明 |
|-----|---------|------|
| 查询操作 | GET | 不修改数据的查询 |
| 创建/提交操作 | POST | 注册、登录、发布资源、上传文件等 |
| 修改操作 | POST | 修改个人信息、修改资源等 |
| 删除操作 | POST | 删除资源、删除用户等 |

> 所有响应体均使用 JSON 格式。

### 5.3 错误码定义

> 所有错误码均通过 `ResponseCodeEnum` 枚举统一管理，以下为各错误码的标准提示信息与实际代码保持一致。

| 错误码 | 枚举常量名 | 含义 | 标准提示信息 |
|-------|-----------|------|-------------|
| 1 | SUCCESS | 成功 | Success! |
| 0 | FAILURE | 通用失败 | 系统繁忙，请稍后重试 |
| 1001 | NOT_LOGGED_IN | 未登录 | 未登录或登录已过期 |
| 1002 | NO_PERMISSION | 无权限 | 无权限执行该操作 |
| 2001 | ACCOUNT_ALREADY_EXISTS | 账号已存在 | 账号已存在 |
| 2002 | ACCOUNT_OR_PASSWORD_ERROR | 账号或密码错误 | 账号或密码错误 |
| 2003 | ACCOUNT_LOCKED | 账号已锁定 | 账号已锁定，请稍后再试 |
| 2004 | INVALID_PASSWORD_FORMAT | 密码格式不合法 | 密码格式不合法：需不少于8位且包含数字和字母 |
| 3001 | RESOURCE_NOT_FOUND | 资源不存在 | 资源不存在 |
| 3002 | RESOURCE_STATUS_NOT_ALLOWED | 资源状态不允许该操作 | 当前资源状态不允许该操作 |
| 4001 | EXCEL_FORMAT_ERROR | Excel 格式错误 | Excel 格式错误 |
| 4002 | EXCEL_DATA_INVALID | Excel 数据校验失败 | Excel 数据校验失败 |
| 5001 | FILE_SIZE_EXCEEDED | 文件大小超限 | 文件大小超限（单个文件不超过 50MB） |
| 5002 | FILE_TYPE_NOT_SUPPORTED | 文件格式不支持 | 文件格式不支持 |
| 5003 | UPLOAD_RATE_LIMITED | 上传频率超限 | 上传频率超限，请稍后再试 |

---

## 六、加分项说明

> **实现状态说明**：✅ 已有框架（对应类/文件已创建）  ⚪ 待实现

| 加分项 | 所属模块 | 功能描述 | 实现建议 | 实现状态 | 对应代码位置 |
|-------|---------|---------|---------|---------|-------------|
| 导入错误处理与数据回滚 | 数据导入 | 批量导入信息时提供完善的错误处理机制和数据回滚功能；导入出错时可快速恢复到导入前状态，并报告给管理员 | 使用 Spring 事务管理（@Transactional），导入失败时回滚；生成错误报告返回管理员 | ✅ 已有框架 | `utils/ExcelUtil.java`、`service/impl/AdminServiceImpl.java`、`components/admin/ImportResult.vue` |
| 在线预览功能 | 资源管理 | 提供在线预览功能，方便师生直接浏览 PPT 等内容 | 可使用 OpenOffice、IOUtils 等工具实现文件在线预览 | ⚪ 待实现 | — |
| 接口限流与防刷机制 | 安全 | 1. 登录防暴力破解：同一账号 5 分钟内连续失败 5 次，锁定 15 分钟；2. 文件上传频率限制：同一用户每分钟最多上传 5 个文件 | 可使用 Redis 计数器 + 定时过期实现；或使用 Guava RateLimiter | ✅ 已有框架 | `aspect/RateLimitAspect.java`（文件上传限流切面）、`entity/User.java`（含 login_fail_count、lock_time 字段） |
| 资源文件去重（秒传） | 资源管理 | 上传文件时先计算 MD5/SHA-256 哈希值，与数据库已有文件哈希比对；若相同文件已存在，直接关联已有记录而非重复存储 | 维护 file_storage 表记录哈希与路径，上传时先查哈希；相同则增加引用计数 | ✅ 已有框架 | `entity/FileStorage.java`、`mapper/FileStorageMapper.java`、`service/FileService.java`、`utils/HashUtil.java`、`common/Constants.java` |

### 6.1 加分项代码框架已建清单

根据当前项目结构，以下加分项对应的骨架代码已经创建，可在后续开发中填充业务逻辑：

**（1）接口限流与防刷机制**
- 切面类：`aspect/RateLimitAspect.java` — 通过 AOP 切面拦截文件上传相关接口，实现频率计数与限制
- 用户实体扩展字段：`entity/User.java` 中已预留 `login_fail_count`（连续登录失败次数）和 `lock_time`（锁定开始时间）字段，支持登录防暴力破解

**（2）资源文件去重（秒传）**
- 文件存储实体：`entity/FileStorage.java` — 对应 `file_storage` 表（含 file_hash、file_path、file_size、reference_count 字段）
- 持久层：`mapper/FileStorageMapper.java` — 秒传数据操作接口
- 业务层：`service/FileService.java` + `service/impl/FileServiceImpl.java` — 文件服务接口与实现
- 哈希工具：`utils/HashUtil.java` — 计算文件 MD5/SHA-256 哈希值

**（3）导入错误处理与数据回滚**
- Excel 工具：`utils/ExcelUtil.java` — Excel 解析与数据校验工具
- 管理端页面组件：`frontend/src/components/admin/ImportResult.vue` — 导入结果展示组件

---

## 附录 ：全局异常处理与日志

| 项目 | 说明 | 对应代码位置 |
|-----|------|-------------|
| 全局异常处理 | 使用 @ControllerAdvice + @ExceptionHandler 实现集中捕获异常，返回用户友好提示 | `exception/GlobalExceptionHandler.java` |
| 自定义业务异常 | 封装业务逻辑异常，携带错误码与提示信息 | `exception/BusinessException.java` |
| 统一响应封装 | 所有接口返回统一的 Result 结构（code + message + data） | `common/Result.java` + `enums/ResponseCodeEnum.java` |
| 日志跟踪 | 通过 SLF4J 组件进行必要的日志打印与跟踪；MyBatis SQL 日志输出到控制台（StdOutImpl） | `application.yml` 中 `mybatis.configuration.log-impl` 配置 |

---

## 附录二：项目完整文件清单与映射

### A. 后端文件清单（backend/src/main/java/com/qingzhi/demo/）

| 序号 | 包/目录 | 文件名 | 对应 PRD 章节 |
|-----|--------|--------|-------------|
| 1 | — | DemoApplication.java | Spring Boot 启动入口 |
| 2 | aspect/ | RateLimitAspect.java | 六、加分项（接口限流） |
| 3 | common/ | Constants.java | 全局常量定义 |
| 4 | common/ | Result.java | 五、5.1 统一响应格式 |
| 5 | common/ | PageResult.java | 三、3.1 分页查询 |
| 6 | config/ | JwtConfig.java | 三、3.2 JWT 鉴权 |
| 7 | config/ | MyBatisConfig.java | 三、3.3 MyBatis 配置 |
| 8 | config/ | WebConfig.java | 注册 JwtInterceptor、跨域配置 |
| 9 | controller/ | AuthController.java | 二、2.1 用户认证模块 |
| 10 | controller/ | UserController.java | 二、2.2.3 普通用户权限 |
| 11 | controller/ | ResourceController.java | 二、2.3 资源管理模块 |
| 12 | controller/ | FavoriteController.java | 二、2.2.3 收藏操作 |
| 13 | controller/ | FileController.java | 文件上传下载 |
| 14 | controller/ | AdminController.java | 二、2.2.2 管理员权限 |
| 15 | dto/request/ | LoginRequest.java | 登录请求参数 |
| 16 | dto/request/ | RegisterRequest.java | 注册请求参数 |
| 17 | dto/request/ | PasswordResetRequest.java | 密码重置请求 |
| 18 | dto/request/ | ResourcePublishRequest.java | 资源发布请求 |
| 19 | dto/request/ | ResourceUpdateRequest.java | 资源修改请求 |
| 20 | dto/request/ | ResourceQueryRequest.java | 资源查询请求（分页+时间范围） |
| 21 | dto/response/ | LoginResponse.java | 登录响应（Token+用户信息） |
| 22 | dto/response/ | UserInfoResponse.java | 用户信息响应 |
| 23 | dto/response/ | ResourceListResponse.java | 资源列表响应 |
| 24 | dto/response/ | ResourceDetailResponse.java | 资源详情响应 |
| 25 | dto/response/ | FavoriteListResponse.java | 收藏列表响应 |
| 26 | entity/ | User.java | 四、4.1 用户表 |
| 27 | entity/ | Resource.java | 四、4.2 资源表 |
| 28 | entity/ | Favorite.java | 四、4.3 收藏表 |
| 29 | entity/ | FileStorage.java | 四、4.4 文件存储表（加分项秒传） |
| 30 | enums/ | RoleEnum.java | 二、2.2.1 角色定义 |
| 31 | enums/ | ResourceStatusEnum.java | 二、2.3.2 资源状态流转 |
| 32 | enums/ | ResponseCodeEnum.java | 五、5.3 错误码定义 |
| 33 | exception/ | BusinessException.java | 自定义业务异常 |
| 34 | exception/ | GlobalExceptionHandler.java | 附录一 全局异常处理 |
| 35 | interceptor/ | JwtInterceptor.java | 二、2.1.2 JWT 鉴权拦截 |
| 36 | mapper/ | UserMapper.java | 用户持久层 |
| 37 | mapper/ | ResourceMapper.java | 资源持久层 |
| 38 | mapper/ | FavoriteMapper.java | 收藏持久层 |
| 39 | mapper/ | FileStorageMapper.java | 文件存储持久层（秒传） |
| 40 | service/ | AuthService.java | 认证业务接口 |
| 41 | service/ | UserService.java | 用户业务接口 |
| 42 | service/ | ResourceService.java | 资源业务接口 |
| 43 | service/ | FavoriteService.java | 收藏业务接口 |
| 44 | service/ | FileService.java | 文件业务接口（秒传） |
| 45 | service/ | AdminService.java | 管理员业务接口 |
| 46 | service/impl/ | AuthServiceImpl.java | 认证业务实现 |
| 47 | service/impl/ | UserServiceImpl.java | 用户业务实现 |
| 48 | service/impl/ | ResourceServiceImpl.java | 资源业务实现 |
| 49 | service/impl/ | FavoriteServiceImpl.java | 收藏业务实现 |
| 50 | service/impl/ | FileServiceImpl.java | 文件业务实现 |
| 51 | service/impl/ | AdminServiceImpl.java | 管理员业务实现 |
| 52 | utils/ | JwtUtil.java | JWT 生成与解析工具 |
| 53 | utils/ | PasswordUtil.java | 密码加密与格式校验 |
| 54 | utils/ | HashUtil.java | 文件哈希计算（秒传） |
| 55 | utils/ | FileUtil.java | 文件处理工具 |
| 56 | utils/ | ExcelUtil.java | Excel 批量导入工具 |
| 57 | vo/ | UserVO.java | 用户视图对象 |
| 58 | vo/ | ResourceVO.java | 资源视图对象 |

### B. 前端文件清单（frontend/src/）

| 序号 | 目录 | 文件名 | 对应功能模块 |
|-----|------|--------|------------|
| 1 | — | App.vue | 根组件 |
| 2 | — | main.js | 入口文件 |
| 3 | api/ | request.js | Axios 封装（拦截器） |
| 4 | api/ | auth.js | 登录注册接口 |
| 5 | api/ | user.js | 用户个人信息接口 |
| 6 | api/ | resource.js | 资源 CRUD 接口 |
| 7 | api/ | favorite.js | 收藏接口 |
| 8 | api/ | admin.js | 管理员后台接口 |
| 9 | components/common/ | Pagination.vue | 分页组件 |
| 10 | components/common/ | FileUpload.vue | 文件上传组件 |
| 11 | components/common/ | EmptyState.vue | 空状态组件 |
| 12 | components/common/ | StatusTag.vue | 状态标签组件 |
| 13 | components/resource/ | ResourceCard.vue | 资源卡片组件 |
| 14 | components/resource/ | ResourceFilter.vue | 资源筛选组件（时间+课程） |
| 15 | components/admin/ | ImportResult.vue | Excel 导入结果组件 |
| 16 | directives/ | permission.js | 权限控制指令 |
| 17 | layout/ | Layout.vue | 整体布局容器 |
| 18 | layout/ | Header.vue | 顶部导航栏 |
| 19 | layout/ | Sidebar.vue | 侧边栏菜单 |
| 20 | router/ | index.js | 路由实例 |
| 21 | router/ | routes.js | 路由表配置 |
| 22 | stores/ | index.js | Pinia 入口 |
| 23 | stores/ | userStore.js | 用户状态（Token、角色） |
| 24 | stores/ | appStore.js | 应用全局状态 |
| 25 | styles/ | index.css | 全局样式入口 |
| 26 | styles/ | variables.css | CSS 变量 |
| 27 | utils/ | storage.js | 本地存储封装 |
| 28 | utils/ | format.js | 格式化工具（日期、文件大小） |
| 29 | utils/ | validate.js | 校验工具（密码格式等） |
| 30 | utils/ | permission.js | 权限判断工具 |
| 31 | views/auth/ | Login.vue | 登录页 |
| 32 | views/auth/ | Register.vue | 注册页 |
| 33 | views/dashboard/ | Index.vue | 首页/工作台 |
| 34 | views/resource/ | ResourceList.vue | 资源列表页（分页+筛选） |
| 35 | views/resource/ | ResourceDetail.vue | 资源详情页 |
| 36 | views/resource/ | ResourcePublish.vue | 资源发布页 |
| 37 | views/resource/ | ResourceUpdate.vue | 资源修改页 |
| 38 | views/profile/ | Info.vue | 个人信息页 |
| 39 | views/profile/ | ChangePassword.vue | 修改密码页 |
| 40 | views/profile/ | MyResources.vue | 我的资源页 |
| 41 | views/profile/ | MyFavorites.vue | 我的收藏页 |
| 42 | views/admin/ | UserManage.vue | 用户管理页 |
| 43 | views/admin/ | ResourceManage.vue | 资源管理页 |
| 44 | views/admin/ | ResourceReview.vue | 资源审核页 |
| 45 | views/admin/ | ExcelImport.vue | Excel 批量导入页 |
