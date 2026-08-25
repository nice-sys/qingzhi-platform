🎉 **恭喜！你现在正式站在了"写代码"的起跑线上！**

前面那些"建包、搭结构、分前后端"全是热身运动，从现在开始才是真正开始写这个项目。根据你的 PRD，我帮你规划一个**最合理的开发顺序**，让你每一步都走得踏实，不至于写到一半发现前面漏了东西。

---

## 🗺️ 整体开发路线图（建议按这个顺序来）

我把整个项目拆成 **5 个阶段**，每个阶段都有明确的"完成标志"，做完了再进入下一阶段：

```
阶段一：地基       →  阶段二：用户认证   →  阶段三：核心业务   →  阶段四：管理员后台   →  阶段五：加分项
(数据库+基础类)     (登录+注册+JWT)        (资源+收藏)           (审核+导入+用户管理)     (限流+秒传+预览)
```

---

## 阶段一：地基（先把架子搭稳）⏱️ 预计 1-2 天

**目标：数据库能连上，项目能启动，统一的响应格式有了**

| 顺序 | 做什么 | 关键文件 | 做完怎么验证 |
|------|--------|---------|-------------|
| ① | 创建 MySQL 数据库，执行建表 SQL | `schema.sql` | 在 Navicat/DBeaver 里看到 3-4 张空表 |
| ② | 配置 `application.properties` / `application.yml` | 数据库连接、JWT密钥、文件上传路径 | 启动 Spring Boot，控制台没有红色报错 |
| ③ | 建 `Result.java` + `ResponseCodeEnum.java` | `common/`、`enums/` | 能在其他类里引用 `Result.success()` 不报错 |
| ④ | 建 `GlobalExceptionHandler.java` | `exception/` | 写一个测试接口抛出异常，看返回格式是否统一 |
| ⑤ | 建 `JwtUtil.java` | `utils/` | 写个 main 方法测试生成 Token 和解析 Token 是否正常 |
| ⑥ | 建 `PasswordUtil.java`（MD5加密） | `utils/` | 输入 `Admin2026`，看加密后长度是不是 32 位 |

**✅ 阶段一完成标志：** Spring Boot 启动无报错，访问一个简单的测试接口（如 `GET /ping`）能返回 `{"code":1,"message":"Success!","data":"pong"}`

---

## 阶段二：用户认证（迈过第一道坎）⏱️ 预计 2-3 天

**目标：能注册、能登录、有 JWT 拦截器保护其他接口**

| 顺序 | 做什么 | 关键类 | 做完怎么验证 |
|------|--------|--------|-------------|
| ① | 建 `User.java` 实体 + `UserMapper.java` | `entity/`、`mapper/` | 写个测试方法，能插入一条用户记录到数据库 |
| ② | 建 `AuthService.java` + `AuthServiceImpl.java` | `service/`、`service/impl/` | 调用注册方法，数据库多了一条密码加密后的记录 |
| ③ | 建 `AuthController.java`（注册接口 + 登录接口） | `controller/` | **Postman 测试：** 注册成功返回 code=1，重复注册返回 code=2001 |
| ④ | 建 `JwtInterceptor.java` 并注册到拦截器链 | `interceptor/`、`config/` | 不携带 Token 访问接口，返回 code=1001 |
| ⑤ | 建前端 `api/request.js` + `api/auth.js` + `Login.vue` | 前端 | 能从前端页面走通"注册→登录→跳转首页"全流程 |

**✅ 阶段二完成标志：** 用 Postman 跑通注册 → 登录 → 拿到 Token → 用 Token 访问一个受保护接口，全程返回正确结果。

---

## 阶段三：核心业务（用户能用起来了）⏱️ 预计 3-4 天

**目标：能发布资源、查看资源列表、查看详情、收藏/取消收藏**

| 顺序 | 做什么 | 关键类 | 做完怎么验证 |
|------|--------|--------|-------------|
| ① | 建 `Resource.java` 实体 + `ResourceMapper.java` | `entity/`、`mapper/` | 能手动插入一条资源记录 |
| ② | 建 `ResourceService.java` + 实现类（发布、分页查询、详情、修改、删除） | `service/` | 分页查询能按时间倒序返回正确数据 |
| ③ | 建 `ResourceController.java` | `controller/` | Postman 测试 CRUD 接口，注意资源状态流转逻辑 |
| ④ | 建 `Favorite.java` + `FavoriteMapper.java` + `FavoriteService` + `FavoriteController` | 全套 | 收藏→查收藏列表→取消收藏，联合唯一约束生效 |
| ⑤ | 前端页面：`ResourceList.vue`、`ResourceDetail.vue`、`ResourcePublish.vue` | `views/resource/` | 前端能从列表点进详情，能发布资源 |

**✅ 阶段三完成标志：** 你能在浏览器里完成"登录→发布一个资源→在列表看到它→点进去看详情→收藏它"的完整流程。

---

## 阶段四：管理员后台（权限控制落地）⏱️ 预计 2-3 天

**目标：管理员能审核资源、管理用户、Excel 导入**

| 顺序 | 做什么 | 关键类 | 做完怎么验证 |
|------|--------|--------|-------------|
| ① | 完善路由守卫 + 角色权限拦截 | `router/index.js`、`interceptor/` | 学生账号访问 `/admin/xxx` 返回 code=1002 |
| ② | 建 `AdminController.java` + `AdminService.java` | `controller/`、`service/` | 审核通过→资源变为已通过且所有人可见；审核拒绝→填写理由 |
| ③ | 建用户管理接口（增删改查 + 重置密码） | `UserController.java` + `UserService` | 管理员能查所有用户、重置任意用户密码 |
| ④ | 建 `ExcelUtil.java` + Excel导入接口 | `utils/`、`AdminController` | 上传 Excel，数据库批量插入用户，校验失败时回滚 |
| ⑤ | 前端页面：`UserManage.vue`、`ResourceReview.vue`、`ExcelImport.vue` | `views/admin/` | 管理员能在页面上完成审核操作 |

**✅ 阶段四完成标志：** 管理员登录后，能看到审核列表，点击"通过"或"拒绝+填写理由"，前端界面反馈正确，数据库状态同步更新。

---

## 阶段五：加分项（有精力就做）⏱️ 预计 2-3 天

按 PRD 的加分项优先级：

| 优先级 | 加分项 | 实现难度 | 实现方式 |
|--------|--------|---------|---------|
| ★★★ | 登录防暴力破解 | ⭐⭐ | Redis 存失败计数 + 过期时间 |
| ★★★ | 文件上传限流 | ⭐⭐ | Guava RateLimiter 或 Redis |
| ★★☆ | 秒传（文件去重） | ⭐⭐⭐ | 上传前算 Hash，查 `file_storage` 表 |
| ★☆☆ | 在线预览 | ⭐⭐⭐⭐ | 用 OpenOffice + JodConverter 转 PDF |

**✅ 阶段五完成标志：** 同一账号连续输错 5 次密码，锁定 15 分钟不能再登录。

---

## 📌 给你的实操建议

1. **不要跳顺序**：阶段一没做完就写阶段二，后面全是坑。比如 JWT 密钥没配好，后面所有的登录都返回 500。
2. **每做完一步就用 Postman 测一步**：不要攒一堆代码再测，那时报错你都不知道是哪一行的问题。
3. **前端和后端可以并行**：你写到阶段三的 `ResourceController` 时，就可以让前端同时写 `api/resource.js` 和 `ResourceList.vue`，两边一起调通。
4. **数据库建表 SQL 先跑**：PRD 里已经给了表结构，你只需要转成 SQL 语句执行就行。需要我帮你生成一份完整的建表 SQL 吗？

---

你现在想从**阶段一**的哪一步开始？比如：
- 需要我帮你生成 `schema.sql` 建表语句？
- 还是先帮你写 `application.yml` 的配置模板？
- 或者直接把 `Result.java` + `ResponseCodeEnum.java` + `GlobalExceptionHandler.java` 这三件套的代码给你？

你说一声，我直接给代码，你复制进去就能用。咱们不拖，一步步往前走。😊