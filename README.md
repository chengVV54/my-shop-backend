# My Shop Backend

个人电商项目后端，支持**换物（BARTER）**、**二手（SALE）**、**可卖可换（BOTH）**三种交易模式。

## 技术栈

- Spring Boot 3.2.0
- Java 17
- Maven
- Lombok
- MyBatis-Plus 3.5.5
- MySQL 8.4（mysql-connector-j）

## 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.x

## 数据库

- MySQL 8.4
- 数据库名：`my_shop`
- 用户：`shop` / `shop123456`
- 表：`items`、`barter_requests`、`orders`
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

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/health` | 健康检查 |
| GET | `/api/items` | 物品列表（支持 `?mode=BARTER/SALE/BOTH` 筛选） |
| GET | `/api/items/{id}` | 物品详情 |
| GET | `/api/barters` | 换物申请列表（MySQL） |
| GET | `/api/orders` | 二手订单列表（MySQL） |

调用示例：

```bash
# 全部物品
curl http://localhost:8080/api/items

# 只看换物
curl "http://localhost:8080/api/items?mode=BARTER"

# 物品详情
curl http://localhost:8080/api/items/1

# 换物申请 / 二手订单
curl http://localhost:8080/api/barters
curl http://localhost:8080/api/orders
```

## 数据模型

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
- `priceDiff`：差价，`null` 表示不涉及差价
- `status`：`PENDING` / `ACCEPTED` / `REJECTED` / `DONE` / `CANCELLED`

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

- `code`：业务状态码，`200` 表示成功，`400` 参数校验失败，`500` 服务器内部错误
- `message`：提示信息
- `data`：业务数据，无数据时为 `null`

## 项目结构

```
src/main/java/com/myshop/
├── MyShopApplication.java          # 启动类
├── common/
│   ├── Result.java                 # 统一返回体
│   └── GlobalExceptionHandler.java # 全局异常处理
├── config/
│   └── CorsConfig.java             # 跨域配置（放行 localhost:5173）
├── controller/
│   ├── HealthController.java       # 健康检查
│   ├── ItemController.java         # 物品接口
│   ├── BarterController.java       # 换物申请接口
│   └── OrderController.java        # 二手订单接口
├── entity/
│   ├── Item.java                   # 物品表实体（items）
│   ├── BarterRequest.java          # 换物申请表实体（barter_requests）
│   └── Order.java                  # 二手订单表实体（orders）
├── mapper/
│   ├── ItemMapper.java             # 物品表 Mapper
│   ├── BarterRequestMapper.java    # 换物申请表 Mapper
│   └── OrderMapper.java            # 二手订单表 Mapper
├── model/
│   ├── ItemVO.java                 # 物品视图对象
│   ├── BarterRequestVO.java        # 换物申请视图对象
│   └── OrderVO.java                # 二手订单视图对象
└── service/
    ├── ItemService.java            # 物品业务逻辑（查 MySQL，转 ItemVO）
    ├── BarterService.java          # 换物申请业务逻辑（查 MySQL，转 BarterRequestVO）
    └── OrderService.java           # 二手订单业务逻辑（查 MySQL，转 OrderVO）

src/main/resources/
├── application.yml                 # 数据源、SQL 初始化、MyBatis-Plus 配置
├── schema.sql                      # 建表脚本（启动时自动执行）
└── data.sql                        # 初始数据（启动时自动执行）
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
- [ ] 用户登录
- [ ] 发布物品
- [ ] 换物流程
- [ ] 二手下单
