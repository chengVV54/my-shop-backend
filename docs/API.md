# My Shop Backend 接口文档

- Base URL：`http://localhost:8080`
- 数据格式：请求与响应均为 `application/json;charset=UTF-8`
- 统一返回体：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `code` | int | 业务码：`200` 成功 / `400` 参数或业务失败 / `401` 未登录 / `403` 无权操作 / `500` 服务器内部错误 |
| `message` | string | 提示信息 |
| `data` | any | 业务数据，可能为 `null` |

> 失败响应说明：Controller 内部抛出的参数校验失败与业务异常返回 **HTTP 200 + 业务码**；
> 由 Spring Security 在进入 Controller 之前拦截的未登录 / 越权会额外设置 **HTTP 401 / 403**，
> 响应体结构不变，前端可统一按 `code` 处理。

## 鉴权

需要登录的接口必须带请求头：

```
Authorization: Bearer <token>
```

`token` 由 `POST /api/auth/login` 返回，有效期 7 天（`jwt.expiration`）。

| 接口 | 是否需要登录 |
| --- | --- |
| `GET /api/health` | 否 |
| `POST /api/auth/register` | 否 |
| `POST /api/auth/login` | 否 |
| `GET /api/auth/me` | **是** |
| `GET /api/items`、`GET /api/items/{id}` | 否 |
| `GET /api/barters`、`GET /api/barters/{id}` | **是** |
| `POST /api/barters`、`PUT /api/barters/{id}/accept`、`PUT /api/barters/{id}/reject` | **是** |
| `GET /api/orders` | **是** |
| 其他 `/api/**`（未来的写操作） | **是** |

鉴权失败响应示例（HTTP 401）：

```json
{ "code": 401, "message": "未登录或登录已过期", "data": null }
```

## 认证接口

### POST /api/auth/register

注册新用户，密码由服务端用 BCrypt 加密后入库。

请求体：

| 字段 | 类型 | 必填 | 约束 |
| --- | --- | --- | --- |
| `username` | string | 是 | 3~20 位，全局唯一 |
| `password` | string | 是 | 6~64 位（BCrypt 有效长度上限 72 字节） |

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"wangwu","password":"123456"}'
```

成功响应（HTTP 200，不含密码）：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 3,
    "username": "wangwu",
    "avatar": null,
    "credit": 100,
    "createdAt": "2026-10-11 21:10:00"
  }
}
```

失败响应：

```json
{ "code": 400, "message": "用户名已存在", "data": null }
{ "code": 400, "message": "username 长度需为 3~20 位", "data": null }
{ "code": 400, "message": "password 长度需为 6~64 位", "data": null }
```

### POST /api/auth/login

登录并签发 JWT。

请求体：

| 字段 | 类型 | 必填 |
| --- | --- | --- |
| `username` | string | 是 |
| `password` | string | 是 |

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"zhangsan","password":"123456"}'
```

成功响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwidXNlcm5hbWUiOiJ6aGFuZ3NhbiIsImlhdCI6MTc2MDE5MDAwMCwiZXhwIjoxNzYwNzk0ODAwfQ.xxx",
    "user": {
      "id": 1,
      "username": "zhangsan",
      "avatar": null,
      "credit": 100,
      "createdAt": "2026-10-10 17:42:18"
    }
  }
}
```

失败响应（用户名不存在与密码错误返回同一提示，避免用户名被枚举）：

```json
{ "code": 401, "message": "用户名或密码错误", "data": null }
```

JWT 载荷约定：

```json
{ "sub": "1", "username": "zhangsan", "iat": 1760190000, "exp": 1760794800 }
```

### GET /api/auth/me

获取当前登录用户信息，需要登录。

```bash
TOKEN="<login 返回的 token>"
curl http://localhost:8080/api/auth/me -H "Authorization: Bearer $TOKEN"
```

成功响应（结构与注册一致，不含密码）：

```json
{
  "code": 200,
  "message": "success",
  "data": { "id": 1, "username": "zhangsan", "avatar": null, "credit": 100, "createdAt": "2026-10-10 17:42:18" }
}
```

失败响应：

- 无 token / token 失效：HTTP 401 `{"code":401,"message":"未登录或登录已过期","data":null}`
- token 有效但用户已被删除：HTTP 200 `{"code":401,"message":"登录用户不存在","data":null}`

## 业务接口

### GET /api/health

健康检查，无需登录。

```json
{ "code": 200, "message": "success", "data": "ok" }
```

### GET /api/items

物品列表，无需登录。可选参数 `mode`：`BARTER` / `SALE` / `BOTH`，不传返回全部，按 id 升序。

```bash
curl http://localhost:8080/api/items
curl "http://localhost:8080/api/items?mode=BARTER"
```

### GET /api/items/{id}

物品详情，无需登录。不存在时返回 `{"code":500,"message":"物品不存在","data":null}`。

```bash
curl http://localhost:8080/api/items/1
```

ItemVO 字段说明见项目根目录 `README.md` 的「数据模型」章节。

### GET /api/barters

我的换物列表，**需要登录**。只返回与当前登录用户相关的申请，身份取自 JWT，
无法通过参数查询别人的换物申请。结果按 `createdAt` 倒序（同一时刻再按 `id` 倒序）。

- 请求头：`Authorization: Bearer <token>`

查询参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `type` | string | 否 | `from` = 我发起的；`to` = 我收到的；不传、空串或 `all` = 两者合并（行不会重复）；大小写不敏感，其他值返回 `400` |

```bash
TOKEN="<login 返回的 token>"

# 不传 type：我发起的 + 我收到的
curl http://localhost:8080/api/barters -H "Authorization: Bearer $TOKEN"

# 只看我发起的
curl "http://localhost:8080/api/barters?type=from" -H "Authorization: Bearer $TOKEN"

# 只看我收到的
curl "http://localhost:8080/api/barters?type=to" -H "Authorization: Bearer $TOKEN"
```

成功响应（HTTP 200，以 `zhangsan`（id=1）为例，`type=all`）：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 2,
      "fromUserId": 1,
      "toUserId": 2,
      "fromItemId": 5,
      "toItemId": 2,
      "message": "用 Switch OLED 换你的三体全集，不用补差价，你看行吗？",
      "priceDiff": 0.00,
      "status": "ACCEPTED",
      "createdAt": "2026-10-10 17:42:18"
    },
    {
      "id": 1,
      "fromUserId": 2,
      "toUserId": 1,
      "fromItemId": 6,
      "toItemId": 1,
      "message": "想用全新机械键盘换你的相机，我补 500 差价，可以详聊。",
      "priceDiff": 500.00,
      "status": "PENDING",
      "createdAt": "2026-10-10 17:42:18"
    }
  ]
}
```

没有匹配数据时 `data` 为空数组 `[]`，不会返回 `null`。

错误码（HTTP 200 + 业务码，除非另有说明）：

| code | message | 触发条件 |
| --- | --- | --- |
| `401` | `未登录或登录已过期` | 未带 token / token 失效（由 Spring Security 拦截，HTTP 也是 401） |
| `401` | `请先登录` | 进入 Service 后当前用户为空（兜底保护） |
| `400` | `无效的 type 参数` | `type` 不是 `from` / `to` / `all`（大小写不敏感） |

失败响应示例：

```json
{ "code": 400, "message": "无效的 type 参数", "data": null }
```

### POST /api/barters

发起换物申请（最小版本只支持 **1:1**：用我的一件物品换对方的一件物品），**需要登录**。

- 请求头：`Authorization: Bearer <token>`、`Content-Type: application/json`
- 发起方身份取自 JWT，**请求体里没有也不需要 `userId`**：`fromUserId` = 当前登录用户，
  `toUserId` = 目标物品的 `ownerId`
- 新申请的 `status` 固定为 `PENDING`，`priceDiff` 固定为 `0.00`（本版本不计算差价）

请求体：

| 字段 | 类型 | 必填 | 约束 | 说明 |
| --- | --- | --- | --- | --- |
| `toItemId` | long | 是 | 物品必须存在、`status=AVAILABLE`、非本人所有 | 想换取的物品 ID（对方的） |
| `fromItemId` | long | 是 | 物品必须存在、`status=AVAILABLE`、`ownerId` = 当前用户 | 用来交换的物品 ID（自己的） |
| `message` | string | 否 | 最长 500 字 | 申请留言 |

```bash
TOKEN="<login 返回的 token>"
# zhangsan(id=1) 用自己 id=5 的 Switch OLED，换 lisi(id=2) 的 id=2《三体全集》
curl -X POST http://localhost:8080/api/barters \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"toItemId":2,"fromItemId":5,"message":"想用 Switch OLED 换你的三体全集，可以详聊"}'
```

成功响应（HTTP 200）：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 3,
    "fromUserId": 1,
    "toUserId": 2,
    "fromItemId": 5,
    "toItemId": 2,
    "message": "想用 Switch OLED 换你的三体全集，可以详聊",
    "priceDiff": 0.00,
    "status": "PENDING",
    "createdAt": "2026-10-16 23:55:12"
  }
}
```

错误码（HTTP 200 + 业务码，除非另有说明）：

| code | message | 触发条件 |
| --- | --- | --- |
| `401` | `未登录或登录已过期` | 未带 token / token 失效（由 Spring Security 拦截，HTTP 也是 401） |
| `401` | `请先登录` | 进入 Service 后当前用户为空（兜底保护） |
| `400` | `toItemId 不能为空` / `fromItemId 不能为空` | 必填字段缺失或为 `null` |
| `400` | `message 长度不能超过 500 字` | 留言超过 500 字 |
| `404` | `物品不存在` | `toItemId` 或 `fromItemId` 对应的物品不存在 |
| `400` | `物品不可换` | 物品 `status` 不是 `AVAILABLE`（`LOCKED` / `DONE` / `REMOVED`） |
| `400` | `不能和自己换` | `toItemId` 的物品属于当前用户 |
| `403` | `只能用自己的物品发起换物` | `fromItemId` 的物品不属于当前用户 |
| `400` | `已发起过换物申请，请等待对方处理` | 同一对 `fromItemId` + `toItemId` 已有 `PENDING` 申请 |

失败响应示例：

```json
{ "code": 403, "message": "只能用自己的物品发起换物", "data": null }
{ "code": 400, "message": "已发起过换物申请，请等待对方处理", "data": null }
```

### GET /api/barters/{id}

换物申请详情，**需要登录**。只有该申请的**当事人**能查看：`fromUserId`（发起方）或
`toUserId`（接收方）之一，其他人即使知道 `id` 也会被拒绝（`403 无权查看此换物申请`）。

- 请求头：`Authorization: Bearer <token>`
- 返回结构与列表中的单条元素一致
- 不存在与无权查看是两种不同的业务码，便于前端区分「记录没了」与「不给你看」

```bash
TOKEN="<login 返回的 token>"
curl http://localhost:8080/api/barters/2 -H "Authorization: Bearer $TOKEN"
```

成功响应（HTTP 200，以 id=2、`zhangsan`(1) → `lisi`(2) 的申请为例，双方都能看）：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 2,
    "fromUserId": 1,
    "toUserId": 2,
    "fromItemId": 5,
    "toItemId": 2,
    "message": "用 Switch OLED 换你的三体全集，不用补差价，你看行吗？",
    "priceDiff": 0.00,
    "status": "ACCEPTED",
    "createdAt": "2026-10-10 17:42:18"
  }
}
```

错误码（HTTP 200 + 业务码，除非另有说明）：

| code | message | 触发条件 |
| --- | --- | --- |
| `401` | `未登录或登录已过期` | 未带 token / token 失效（由 Spring Security 拦截，HTTP 也是 401） |
| `401` | `请先登录` | 进入 Service 后当前用户为空（兜底保护） |
| `404` | `换物申请不存在` | `id` 对应的申请不存在 |
| `403` | `无权查看此换物申请` | 当前用户既不是发起方也不是接收方 |

失败响应示例：

```json
{ "code": 404, "message": "换物申请不存在", "data": null }
{ "code": 403, "message": "无权查看此换物申请", "data": null }
```

### PUT /api/barters/{id}/accept

**接收方同意**换物申请，**需要登录**。

- 请求头：`Authorization: Bearer <token>`，无请求体
- 权限：只有 `toUserId` 本人能操作，发起方自己也不能替对方同意，其他人更不行
- 状态流转：`PENDING` → `ACCEPTED`；只有 `PENDING` 的申请能被处理，已同意 / 已拒绝 / 已完成再次调用返回 `400 该申请已被处理`
- 副作用：同意成功后，双方物品（`fromItemId`、`toItemId`）的 `status` 被置为 `LOCKED`，
  这样其它 PENDING 申请就无法再拿这两件物品发起新的换物（发起时的「物品不可换」校验会拦住）
- 重复调用不会把 `ACCEPTED` 改成别的值：第二次调用会稳定返回 `400 该申请已被处理`，不会产生副作用

```bash
# 假设 id=2 仍为 PENDING（种子数据里它已初始化为 ACCEPTED，直接调用会得到 400 该申请已被处理）
TOKEN="<lisi 的 token>"
curl -X PUT http://localhost:8080/api/barters/2/accept -H "Authorization: Bearer $TOKEN"
```

成功响应（HTTP 200）：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 2,
    "fromUserId": 1,
    "toUserId": 2,
    "fromItemId": 5,
    "toItemId": 2,
    "message": "用 Switch OLED 换你的三体全集，不用补差价，你看行吗？",
    "priceDiff": 0.00,
    "status": "ACCEPTED",
    "createdAt": "2026-10-10 17:42:18"
  }
}
```

错误码（HTTP 200 + 业务码，除非另有说明）：

| code | message | 触发条件 |
| --- | --- | --- |
| `401` | `未登录或登录已过期` | 未带 token / token 失效（由 Spring Security 拦截，HTTP 也是 401） |
| `401` | `请先登录` | 进入 Service 后当前用户为空（兜底保护） |
| `404` | `换物申请不存在` | `id` 对应的申请不存在 |
| `403` | `只有接收方能处理该申请` | 当前用户不是 `toUserId`（包括发起方本人） |
| `400` | `该申请已被处理` | 当前状态不是 `PENDING`（已同意 / 已拒绝 / 已完成 / 已取消） |

失败响应示例：

```json
{ "code": 403, "message": "只有接收方能处理该申请", "data": null }
{ "code": 400, "message": "该申请已被处理", "data": null }
```

### PUT /api/barters/{id}/reject

**接收方拒绝**换物申请，**需要登录**。

- 请求头：`Authorization: Bearer <token>`，无请求体
- 权限：与 accept 相同，只有 `toUserId` 本人能操作
- 状态流转：`PENDING` → `REJECTED`；同样只接受 `PENDING` 的申请
- 副作用：**不改动物品状态**，双方物品继续保持 `AVAILABLE`，可以继续和别人换

```bash
# 同样假设 id=2 仍为 PENDING，实际可用刚创建的新申请替换该 id
TOKEN="<lisi 的 token>"
curl -X PUT http://localhost:8080/api/barters/2/reject -H "Authorization: Bearer $TOKEN"
```

成功响应（HTTP 200，仅 `status` 变为 `REJECTED`，其余字段不变）：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 2,
    "fromUserId": 1,
    "toUserId": 2,
    "fromItemId": 5,
    "toItemId": 2,
    "message": "用 Switch OLED 换你的三体全集，不用补差价，你看行吗？",
    "priceDiff": 0.00,
    "status": "REJECTED",
    "createdAt": "2026-10-10 17:42:18"
  }
}
```

错误码与 accept 完全一致（`401` / `404` / `403` / `400 该申请已被处理`）。

### GET /api/orders

二手订单列表，**需要登录**，按 id 升序。

```bash
curl http://localhost:8080/api/orders -H "Authorization: Bearer $TOKEN"
```

## 测试账号

| id | username | password | 说明 |
| --- | --- | --- | --- |
| 1 | `zhangsan` | `123456` | 对应 items 1 / 3 / 5 的 `owner_id` |
| 2 | `lisi` | `123456` | 对应 items 2 / 4 / 6 的 `owner_id` |

`schema.sql` 与 `data.sql` 在应用启动时自动执行，无需手工插入。

## 安全约定

- 密码只以 BCrypt 哈希存储，任何接口都不返回 `password`
- JWT 密钥从配置读取（`jwt.secret`，可用环境变量 `JWT_SECRET` 覆盖），不写死在代码里
- 登录失败不区分「用户不存在」与「密码错误」，避免用户名枚举
- 修改用户自有数据的接口，必须在 Service 层用 `UserContext.getCurrentUserId()`
  与数据行的所有者字段（`owner_id` / `seller_id` / `to_user_id`）比对，禁止信任请求体里的用户 ID
