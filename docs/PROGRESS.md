# 开发进度

> 说明：本仓库原先没有 `docs/` 目录，架构与约定集中写在根目录 `README.md`，
> 这里只记录进度，接口细节见 [API.md](./API.md)。

## 已完成

- [x] 项目骨架（Spring Boot 3.2 + Java 17 + Maven + Lombok）
- [x] 健康检查接口 `GET /api/health`
- [x] 物品接口（假数据 → 改为 MySQL）
- [x] 换物接口（假数据 → 改为 MySQL）
- [x] 订单接口（假数据 → 改为 MySQL）
- [x] 数据库接入（MySQL 8 + MyBatis-Plus 3.5.5，`schema.sql` / `data.sql` 启动自动执行）
- [x] 统一返回体 `Result` 与全局异常处理
- [x] CORS 放行前端开发服务器 `localhost:5173`
- [x] **用户系统（后端）**
  - [x] `users` 表（username 唯一索引，password 存 BCrypt 哈希，credit 默认 100）
  - [x] `User` 实体 + `UserMapper`
  - [x] `JwtUtil`（jjwt 0.12.5，密钥与有效期来自配置，支持 `JWT_SECRET` 环境变量覆盖）
  - [x] `SecurityConfig`：关闭 CSRF、无状态 Session、按路径授权、BCryptPasswordEncoder、401/403 统一 JSON
  - [x] `JwtAuthenticationFilter`：解析 `Authorization: Bearer <token>` 并写入登录上下文
  - [x] `UserContext`：ThreadLocal 保存当前用户 ID，请求结束清理
  - [x] `AuthService`：`register` / `login` / `getCurrentUser`
  - [x] `AuthController`：`POST /api/auth/register`、`POST /api/auth/login`、`GET /api/auth/me`
  - [x] 请求体校验（username 3~20 位，password 6~64 位）
  - [x] 接口保护：`/api/barters`、`/api/orders`、`/api/auth/me` 需登录；`GET /api/items` 保持匿名
  - [x] 两个测试用户（`zhangsan` / `lisi`，密码 `123456`，存 BCrypt 哈希）
  - [x] 文档：`README.md`、`docs/API.md`
- [x] **发起换物 `POST /api/barters`（1:1）**
  - [x] `CreateBarterRequest`（`toItemId` / `fromItemId` 必填，`message` ≤500 字）
  - [x] `BarterService#create`：登录校验 → 目标物品校验 → 我方物品归属校验 → 重复申请拦截 → 落库
  - [x] 发起方身份取自 `UserContext`，`toUserId` 由目标物品 `ownerId` 推导，不接受前端传 userId
  - [x] 新申请固定 `status=PENDING`、`priceDiff=0`
  - [x] 文档：`README.md`、`docs/API.md`、`docs/ARCHITECTURE.md`
- [x] **我的换物列表 `GET /api/barters?type=from/to/all`**
  - [x] 删除原「查全表」的假数据逻辑，改为按当前用户查询真实库表
  - [x] `type=from` 查 `from_user_id`、`type=to` 查 `to_user_id`、`all` 两者 OR 合并（不传默认 `all`）
  - [x] 按 `created_at` 倒序，同一时刻按 `id` 倒序，保证顺序稳定
  - [x] 未登录 `401 请先登录`，非法 `type` 返回 `400 无效的 type 参数`
  - [x] 文档：`README.md`、`docs/API.md`、`docs/ARCHITECTURE.md`

## 待办

- [ ] 发布物品（`POST /api/items`，登录 + `ownerId` 取自 `UserContext`）
- [ ] 编辑 / 下架物品（`PUT` / `DELETE /api/items/{id}`，需校验 `owner_id` 归属）
- [ ] 换物流程：接受 / 拒绝 / 完成（发起已完成，见上方）
  - [ ] 接受 / 拒绝接口（只有 `toUserId` 本人能操作）
  - [ ] 接受后把双方物品置为 `LOCKED`，完成时置为 `DONE`
  - [ ] `(from_item_id, to_item_id, status)` 层面的并发兜底（应用层查重存在并发窗口）
- [ ] 二手下单与订单状态流转（买家不能买自己的物品）
- [ ] 头像上传与用户资料修改
- [ ] 积分（`credit`）的增减规则
- [ ] 接口层越权用例的集成测试

## 约定与注意事项

- 所有写操作都必须先做登录校验（`SecurityConfig` 已按路径拦截），再做**归属校验**：
  `UserContext.getCurrentUserId()` 与数据行的 `owner_id` / `seller_id` / `to_user_id` 比对，
  不相等返回 `403`，禁止直接使用请求体里的用户 ID。
- `schema.sql` / `data.sql` 每次启动都会执行，必须保持幂等：
  建表用 `CREATE TABLE IF NOT EXISTS`，插数据用 `INSERT IGNORE` + 固定主键。
- 新增依赖后需同步更新 `README.md` 的技术栈与项目结构。
- 密码相关的任何改动都不得把明文或哈希写进日志、响应体。
