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
| `GET /api/barters` | **是** |
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

换物申请列表，**需要登录**，按 id 升序。

```bash
curl http://localhost:8080/api/barters -H "Authorization: Bearer $TOKEN"
```

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
