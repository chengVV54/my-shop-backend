# My Shop Backend

个人电商项目后端，支持**换物（BARTER）**、**二手（SALE）**、**可卖可换（BOTH）**三种交易模式。

## 技术栈

- Spring Boot 3.2.0
- Java 17
- Maven
- Lombok
- MyBatis-Plus 3.5.5
- Spring Security 6（JWT 无状态鉴权 + BCrypt 密码加密）
- jjwt 0.12.5（JWT 签发与校验）
- MySQL 8.4（mysql-connector-j）

## 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.x

## 数据库

- MySQL 8.4
- 数据库名：`my_shop`
- 用户：`shop` / `shop123456`
- 表：`users`、`items`、`barter_requests`、`orders`
- 建表脚本：`src/main/resources/schema.sql`
- 初始数据：`src/main/resources/data.sql`

连接配置位于 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: "jdbc:mysql://localhost:3306/my_shop?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
    username: shop
    password: shop123456
    driver-class-name: com.mysql.cj.jdbc.Driver
  sql:
    init:
      mode: always
      schema-locations: classpath:schema.sql
      data-locations: classpath:data.sql
      encoding: UTF-8
      continue-on-error: false
```

### 表结构

**users（用户）**

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | BIGINT AUTO_INCREMENT | 主键 |
| `username` | VARCHAR(50) NOT NULL UNIQUE | 登录名，全局唯一 |
| `password` | VARCHAR(100) NOT NULL | **BCrypt 哈希**（60 字符，`$2a$10$...`），从不存明文 |
| `avatar` | VARCHAR(255) | 头像地址，可为空 |
| `credit` | INT | 积分，默认 100 |
| `created_at` | DATETIME | 注册时间 |

**items（物品）**

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | BIGINT AUTO_INCREMENT | 主键 |
| `title` | VARCHAR(200) NOT NULL | 标题 |
| `images` | TEXT | 图片地址列表，JSON 字符串，如 `["a.jpg"]` |
| `description` | TEXT | 详细描述 |
| `category` | VARCHAR(50) NOT NULL | 分类 |
| `` `condition` `` | VARCHAR(50) NOT NULL | 成色。MySQL 保留字，SQL 中必须用反引号 |
| `owner_id` | BIGINT NOT NULL | 发布者用户 ID |
| `mode` | VARCHAR(20) NOT NULL | BARTER / SALE / BOTH |
| `price` | DECIMAL(10,2) | 售价，仅 SALE / BOTH 有值 |
| `want_categories` | TEXT | 想换的分类，JSON 字符串 |
| `want_description` | VARCHAR(500) | 想换什么的补充描述 |
| `allow_price_diff` | TINYINT(1) | 是否接受补差价 |
| `status` | VARCHAR(20) NOT NULL | AVAILABLE / LOCKED / DONE / REMOVED |
| `created_at` | DATETIME | 创建时间 |

**barter_requests（换物申请）**

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | BIGINT AUTO_INCREMENT | 主键 |
| `from_user_id` | BIGINT NOT NULL | 发起方用户 ID |
| `to_user_id` | BIGINT NOT NULL | 接收方用户 ID |
| `from_item_id` | BIGINT NOT NULL | 发起方提供的物品 ID |
| `to_item_id` | BIGINT NOT NULL | 想换取的物品 ID |
| `message` | VARCHAR(500) | 申请留言 |
| `price_diff` | DECIMAL(10,2) | 差价，NULL 表示不涉及差价 |
| `status` | VARCHAR(20) NOT NULL | PENDING / ACCEPTED / REJECTED / DONE / CANCELLED |
| `created_at` | DATETIME | 创建时间 |

**orders（二手订单）**

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | BIGINT AUTO_INCREMENT | 主键 |
| `buyer_id` | BIGINT NOT NULL | 买家用户 ID |
| `seller_id` | BIGINT NOT NULL | 卖家用户 ID |
| `item_id` | BIGINT NOT NULL | 物品 ID |
| `price` | DECIMAL(10,2) NOT NULL | 成交价 |
| `address` | VARCHAR(200) | 收货地址 |
| `status` | VARCHAR(20) NOT NULL | PENDING / PAID / SHIPPED / DONE / CANCELLED |
| `created_at` | DATETIME | 下单时间 |

### 数据与字段约定

- **脚本自动执行**：应用每次启动都会执行 `schema.sql` 与 `data.sql`（`spring.sql.init.mode=always`）。
  两个脚本都是幂等的：建表用 `CREATE TABLE IF NOT EXISTS`，插数据用 `INSERT IGNORE` + 固定主键
  （items 用 1~6，barter_requests / orders 用 1~2），所以重复启动不会重复插入，也不会覆盖你手动改过的行。
- **JSON 字段**：`items.images` 与 `items.want_categories` 是列表，MySQL 里以 JSON 字符串存储
  （如 `["PHONE","COMPUTER"]`），由 `ItemService` 用 Jackson 在 `List<String>` 与字符串之间互转。
  其中 `images` 为空时按空列表返回（前端可直接遍历），`wantCategories` 为空时保持 `null`
  （表示该模式下「不适用」，例如只卖不换的物品）。
- **保留字**：`condition` 是 MySQL 保留字，实体上通过 `@TableField("`condition`")` 转义，
  写原生 SQL 时同样要加反引号。

## 用户系统与鉴权

后端自带一套完整的用户系统：注册、登录、获取当前用户，采用 **JWT 无状态鉴权 + BCrypt 密码加密**，
未引入 Session / Cookie，天然适配前后端分离。

### 密码怎么存

- 注册时用 `BCryptPasswordEncoder` 加密，库里只存 60 字符的哈希（`$2a$10$...`），**任何接口都不会返回密码字段**
- BCrypt 自带随机盐，同一个密码每次加密结果都不同，无法通过比对视图中哈希来猜测密码
- 登录用 `passwordEncoder.matches(明文, 哈希)` 校验，不自己做字符串比较

### JWT 怎么用

1. `POST /api/auth/login` 拿到 `token`
2. 之后每个需要登录的请求都带上请求头：`Authorization: Bearer <token>`
3. 后端 `JwtAuthenticationFilter` 校验签名与有效期，把 `userId` / `username` 放进
   `SecurityContext` 与 `UserContext`（ThreadLocal），请求结束立即清理
4. Token 载荷只有 `sub`（userId）与 `username`，**身份永远来自签名校验过的 Token，不信任前端传来的 userId**

密钥与有效期配置在 `application.yml`，代码里不写死：

```yaml
jwt:
  secret: ${JWT_SECRET:myShopSecretKeyForJwtTokenMustBeAtLeast32BytesLong123456}
  expiration: ${JWT_EXPIRATION:604800000}   # 7 天
```

生产环境请通过环境变量注入真实密钥：`export JWT_SECRET=<至少 32 字节的随机串>`。
默认值仅供本地开发，HS256 要求密钥不少于 32 字节，太短会在启动时直接报错。

### 哪些接口要登录

| 接口 | 是否需要登录 |
| --- | --- |
| `GET /api/health` | 否 |
| `POST /api/auth/register`、`POST /api/auth/login` | 否 |
| `GET /api/items`、`GET /api/items/{id}` | 否（浏览商品无需登录） |
| `GET /api/auth/me` | **是** |
| `GET /api/barters`、`GET /api/barters/{id}`、`POST /api/barters` | **是** |
| `PUT /api/barters/{id}/accept`、`PUT /api/barters/{id}/reject` | **是** |
| `GET /api/orders` | **是** |
| 其他 `/api/**`（为后续发布物品、下单等写操作预留） | **是** |

鉴权失败由 Spring Security 统一返回 JSON：

- 未登录 / Token 失效：HTTP **401** + `{"code":401,"message":"未登录或登录已过期","data":null}`
- 已登录但无权操作：HTTP **403** + `{"code":403,"message":"没有权限执行该操作","data":null}`

> 越权防护约定：凡是修改「用户自己的数据」的操作（编辑物品、处理换物申请、取消订单等），
> Service 层必须先取出 `UserContext.getCurrentUserId()`，与数据行的 `owner_id` / `seller_id` / `to_user_id`
> 比对通过后才执行，禁止直接信任请求体里的用户 ID。

### 测试账号

`schema.sql` 建表、`data.sql` 初始化，应用启动即可用下面两个账号登录（密码都是 `123456`）：

| id | username | password（明文） | 说明 |
| --- | --- | --- | --- |
| 1 | `zhangsan` | `123456` | 与 items 1/3/5 的 `owner_id` 对应 |
| 2 | `lisi` | `123456` | 与 items 2/4/6 的 `owner_id` 对应 |

对应的 BCrypt 哈希（`data.sql` 中实际写入的值，均已验证 `123456` 可通过校验）：

```
zhangsan  $2a$10$GPzfmy1XK8Aj3v87qlslTOf8cjsXO2jGyw9ZG1qLCtenc9LApryq2
lisi      $2a$10$SGNjrLinWwqMuR5iq9gXKOSLZJafDuLETVJtpaqyWGcH5USJwi3lW
```

## 启动前

1. 确保 MySQL 已启动：`sudo systemctl start mysql`
2. 确保数据库和用户存在（已配置好）
3. `mvn spring-boot:run` 会自动执行 `schema.sql` 和 `data.sql`

## 快速开始

```bash
# 开发模式启动，默认监听 8080 端口
mvn spring-boot:run

# 打包
mvn clean package

# 运行打包后的 jar
java -jar target/*.jar
```

启动后访问健康检查接口验证：

```bash
curl http://localhost:8080/api/health
```

## 接口列表

| 方法 | 路径 | 说明 | 登录 |
| --- | --- | --- | --- |
| GET | `/api/health` | 健康检查 | 否 |
| POST | `/api/auth/register` | 注册 | 否 |
| POST | `/api/auth/login` | 登录，返回 JWT | 否 |
| GET | `/api/auth/me` | 当前登录用户信息 | 是 |
| GET | `/api/items` | 物品列表（支持 `?mode=BARTER/SALE/BOTH` 筛选） | 否 |
| GET | `/api/items/{id}` | 物品详情 | 否 |
| GET | `/api/barters` | 我的换物列表（支持 `?type=from/to/all`） | 是 |
| GET | `/api/barters/{id}` | 换物申请详情（仅当事人可见） | 是 |
| POST | `/api/barters` | 发起换物（1:1，`toItemId` 换 `fromItemId`） | 是 |
| PUT | `/api/barters/{id}/accept` | 接收方同意换物（`PENDING → ACCEPTED`，双方物品置 `LOCKED`） | 是 |
| PUT | `/api/barters/{id}/reject` | 接收方拒绝换物（`PENDING → REJECTED`） | 是 |
| GET | `/api/orders` | 二手订单列表（MySQL） | 是 |

调用示例：

```bash
# 全部物品
curl http://localhost:8080/api/items

# 只看换物
curl "http://localhost:8080/api/items?mode=BARTER"

# 物品详情
curl http://localhost:8080/api/items/1

# 注册
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"wangwu","password":"123456"}'

# 登录，拿 token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"zhangsan","password":"123456"}'

# 带 token 访问需要登录的接口
TOKEN="<上一步返回的 token>"
curl http://localhost:8080/api/auth/me      -H "Authorization: Bearer $TOKEN"
curl http://localhost:8080/api/orders       -H "Authorization: Bearer $TOKEN"

# 我的换物列表：不传 type 或 type=all = 我发起的 + 我收到的
curl http://localhost:8080/api/barters -H "Authorization: Bearer $TOKEN"
# 只看我发起的
curl "http://localhost:8080/api/barters?type=from" -H "Authorization: Bearer $TOKEN"
# 只看我收到的
curl "http://localhost:8080/api/barters?type=to" -H "Authorization: Bearer $TOKEN"

# 发起换物：zhangsan(id=1) 用自己 id=5 的 Switch，换 lisi(id=2) 的 id=2《三体全集》
curl -X POST http://localhost:8080/api/barters \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"toItemId":2,"fromItemId":5,"message":"想用 Switch OLED 换你的三体全集，可以详聊"}'

# 换物详情：只有当事人（发起方 / 接收方）能看，局外人 → 403 无权查看此换物申请
curl http://localhost:8080/api/barters/1 -H "Authorization: Bearer $TOKEN"

# 同意 / 拒绝：只有接收方能操作。种子数据 id=1 是 lisi 发起、zhangsan 接收，所以用 zhangsan 的 token
curl -X PUT http://localhost:8080/api/barters/1/accept -H "Authorization: Bearer $TOKEN"
# 注意：同一条申请被处理后，再调用 /accept 或 /reject 都会返回 400 该申请已被处理
curl -X PUT http://localhost:8080/api/barters/1/reject -H "Authorization: Bearer $TOKEN"

# 不带 token 访问受保护接口 → HTTP 401
curl -i http://localhost:8080/api/orders
```

登录返回示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
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

## 数据模型

### UserVO（用户）

- `id` / `username` / `avatar` / `credit` / `createdAt`，**不含 `password`**
- 由 `AuthService` 从 `User` 实体转换而来，接口一律返回 VO，不直接序列化实体

### ItemVO（物品）

- `mode`：`BARTER`（换物）/ `SALE`（二手）/ `BOTH`（都可）
- `category`：`CAMERA` / `BOOK` / `INSTRUMENT` / `GAME` / `PHONE` / `COMPUTER` / `CLOTHING` / `OTHER`
- `condition`：`NEW` / `LIKE_NEW` / `GOOD` / `FAIR` / `POOR`
- `status`：`AVAILABLE` / `LOCKED` / `DONE` / `REMOVED`

其中换物相关字段（`wantCategories`、`wantDescription`、`allowPriceDiff`）仅在 `mode` 为 `BARTER` 或 `BOTH` 时有值；
售价字段 `price` 仅在 `mode` 为 `SALE` 或 `BOTH` 时有值。

### BarterRequestVO（换物申请）

- `fromUserId` / `toUserId`：发起方与接收方用户 ID
- `fromItemId` / `toItemId`：发起方提供的物品与想换取的物品
- `priceDiff`：差价，`null` 表示不涉及差价；发起换物时会固定写成 `0.00`
- `status`：`PENDING` / `ACCEPTED` / `REJECTED` / `DONE` / `CANCELLED`
  - 状态流转：发起 → `PENDING`；接收方同意 → `ACCEPTED`（双方物品同时置 `LOCKED`）；接收方拒绝 → `REJECTED`（物品状态不变）
  - 只有 `PENDING` 能被处理，已处理的申请再次操作返回 `400 该申请已被处理`
- 发起换物只支持 **1:1**：请求体传 `toItemId`（想换的）+ `fromItemId`（自己提供的），
  `fromUserId` 取自登录态、`toUserId` 由目标物品的 `ownerId` 推导，两者都不接受前端传值
- 越权边界：详情只对 `fromUserId` / `toUserId` 开放；同意 / 拒绝只对接收方 `toUserId` 开放，
  发起方不能替对方点头

### OrderVO（二手订单）

- `buyerId` / `sellerId` / `itemId`：买家、卖家与物品 ID
- `price`：成交价
- `address`：收货地址
- `status`：`PENDING` / `PAID` / `SHIPPED` / `DONE` / `CANCELLED`

## 统一返回格式

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

- `code`：业务状态码，`200` 表示成功，`400` 参数校验失败 / 业务失败，`401` 未登录，
  `403` 无权操作，`500` 服务器内部错误
- `message`：提示信息
- `data`：业务数据，无数据时为 `null`

补充说明：Controller 层返回的失败（参数校验、业务异常）走的是 HTTP 200 + 业务码；
而 **Spring Security 在进入 Controller 之前拦截的未登录 / 越权，会同时设置 HTTP 状态码**
（`401` / `403`），响应体结构保持一致，前端可据此统一跳转登录。

## 项目结构

```
src/main/java/com/myshop/
├── MyShopApplication.java          # 启动类
├── common/
│   ├── Result.java                 # 统一返回体
│   ├── BusinessException.java      # 业务异常（用户名已存在、密码错误等）
│   └── GlobalExceptionHandler.java # 全局异常处理
├── config/
│   ├── CorsConfig.java             # 跨域配置（放行 localhost:5173）
│   ├── SecurityConfig.java         # 安全配置：访问规则、CORS、BCrypt、401/403 JSON
│   ├── JwtAuthenticationFilter.java# 解析 Bearer Token，写入登录上下文
│   └── UserContext.java            # ThreadLocal 保存当前用户 ID
├── controller/
│   ├── HealthController.java       # 健康检查
│   ├── AuthController.java         # 注册 / 登录 / 当前用户
│   ├── ItemController.java         # 物品接口
│   ├── BarterController.java       # 换物申请接口（需登录）
│   └── OrderController.java        # 二手订单接口（需登录）
├── entity/
│   ├── User.java                   # 用户表实体（users）
│   ├── Item.java                   # 物品表实体（items）
│   ├── BarterRequest.java          # 换物申请表实体（barter_requests）
│   └── Order.java                  # 二手订单表实体（orders）
├── mapper/
│   ├── UserMapper.java             # 用户表 Mapper
│   ├── ItemMapper.java             # 物品表 Mapper
│   ├── BarterRequestMapper.java    # 换物申请表 Mapper
│   └── OrderMapper.java            # 二手订单表 Mapper
├── model/
│   ├── UserVO.java                 # 用户视图对象（无 password）
│   ├── RegisterRequest.java        # 注册请求体（校验注解）
│   ├── LoginRequest.java           # 登录请求体
│   ├── LoginResponse.java          # 登录响应（token + user）
│   ├── ItemVO.java                 # 物品视图对象
│   ├── BarterRequestVO.java        # 换物申请视图对象
│   ├── CreateBarterRequest.java    # 发起换物请求体（校验注解，不含 userId）
│   └── OrderVO.java                # 二手订单视图对象
├── util/
│   └── JwtUtil.java                # JWT 签发与校验（jjwt 0.12.x）
└── service/
    ├── AuthService.java            # 注册 / 登录 / 当前用户
    ├── ItemService.java            # 物品业务逻辑（查 MySQL，转 ItemVO）
    ├── BarterService.java          # 换物申请业务逻辑（查 MySQL，转 BarterRequestVO）
    └── OrderService.java           # 二手订单业务逻辑（查 MySQL，转 OrderVO）

src/main/resources/
├── application.yml                 # 数据源、SQL 初始化、MyBatis-Plus、JWT 配置
├── schema.sql                      # 建表脚本（启动时自动执行）
└── data.sql                        # 初始数据（含两个测试用户，启动时自动执行）

docs/
├── API.md                          # 接口文档（含鉴权说明与 curl 示例）
├── ARCHITECTURE.md                 # 目录结构、分层约定与参考写法
└── PROGRESS.md                     # 开发进度
```

## 配套前端仓库

<https://github.com/chengVV54/my-shop-frontend>

## 开发进度

- [x] 项目骨架
- [x] 健康检查接口
- [x] 物品接口（假数据）
- [x] 换物接口（假数据）
- [x] 订单接口（假数据）
- [x] 数据库接入（MySQL + MyBatis-Plus）
- [x] 用户系统（后端）：注册 / 登录 / JWT 鉴权 / 越权防护基础
- [ ] 发布物品
- [x] 换物流程
  - [x] 发起换物 `POST /api/barters`（登录 + 归属校验 + 重复申请拦截）
  - [x] 我的换物列表 `GET /api/barters?type=from/to/all`（只查当前用户相关数据）
  - [x] 换物详情 `GET /api/barters/{id}`（只有当事人能看）
  - [x] 接受 / 拒绝 `PUT /api/barters/{id}/accept`、`/reject`（只有接收方能操作，同意后物品 `LOCKED`）
  - [ ] 完成换物 `ACCEPTED → DONE`
- [ ] 二手下单
