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
- [x] **换物详情 `GET /api/barters/{id}`**
  - [x] 只有当事人（`fromUserId` 或 `toUserId`）能查看，其他人 `403 无权查看此换物申请`
  - [x] 不存在（含 `id` 为空）返回 `404 换物申请不存在`，未登录 `401 请先登录`
  - [x] 返回 `BarterRequestVO`，时间格式化为 `yyyy-MM-dd HH:mm:ss`
- [x] **同意 / 拒绝换物 `PUT /api/barters/{id}/accept`、`PUT /api/barters/{id}/reject`**
  - [x] 只有接收方（`toUserId`）能操作，其他人（含发起方）`403 只有接收方能处理该申请`
  - [x] 只允许 `PENDING` 流转到 `ACCEPTED` / `REJECTED`，其他状态 `400 该申请已被处理`
  - [x] 条件更新（`WHERE id = ? AND status = 'PENDING'`）做并发兜底，双击 / 双端同时操作只有一次成功
  - [x] 同意后把双方物品置为 `LOCKED`，防止被其他人再换走；拒绝不改物品状态
  - [x] 方法加 `@Transactional`，状态更新与物品锁定同生共死
  - [x] 文档：`README.md`、`docs/API.md`、`docs/ARCHITECTURE.md`

- [x] **发布物品 `POST /api/items`（后端）**
  - [x] `CreateItemRequest`（`title` 必填 ≤200、`category` / `condition` / `mode` 必填、`price` 条件必填、`wantDescription` ≤500）
  - [x] `ItemService#create`：登录校验（401）→ `mode` 归一化 → 价格条件校验 → 组装实体 → 落库 → 转 `ItemVO`
  - [x] `ownerId` 取自 `UserContext`，`status` 固定 `AVAILABLE`、`createdAt` 服务端写入
  - [x] `wantCategories` 由 `List<String>` 序列化成 JSON 字符串入库
  - [x] `ItemController#create`：`POST /api/items`，`@Valid @RequestBody`，返回 `Result<ItemVO>`
  - [x] 文档：`docs/API.md`、`docs/PROGRESS.md`

## 待办

- [ ] 编辑 / 下架物品（`PUT` / `DELETE /api/items/{id}`，需校验 `owner_id` 归属）
- [ ] 换物流程
  - [x] 发起（`POST /api/barters`）
  - [x] 详情（`GET /api/barters/{id}`）
  - [x] 接受 / 拒绝（`PUT /api/barters/{id}/accept`、`/reject`）
  - [ ] 完成换物：`ACCEPTED` → `DONE`，双方物品 `LOCKED` → `DONE`
  - [ ] 取消换物：发起方撤回 `PENDING` 申请（`CANCELLED`）
  - [ ] 同意后把同一对物品的其它 `PENDING` 申请自动置为 `REJECTED`（当前靠物品 `LOCKED` 间接拦住新申请）
  - [ ] `(from_item_id, to_item_id, status)` 层面的并发兜底（应用层查重 + 本次条件更新已缓解，DB 唯一索引仍未加）

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
