# My Shop Backend

个人电商项目后端，支持**换物（BARTER）**、**二手（SALE）**、**可卖可换（BOTH）**三种交易模式。

## 技术栈

- Spring Boot 3.2.0
- Java 17
- Maven
- Lombok

## 环境要求

- JDK 17+
- Maven 3.8+

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
| GET | `/api/barters` | 换物申请列表（假数据） |
| GET | `/api/orders` | 二手订单列表（假数据） |

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
├── model/
│   ├── ItemVO.java                 # 物品视图对象
│   ├── BarterRequestVO.java        # 换物申请视图对象
│   └── OrderVO.java                # 二手订单视图对象
└── service/
    ├── ItemService.java            # 物品业务逻辑（当前为假数据）
    ├── BarterService.java          # 换物申请业务逻辑（当前为假数据）
    └── OrderService.java           # 二手订单业务逻辑（当前为假数据）
```

## 配套前端仓库

<https://github.com/chengVV54/my-shop-frontend>

## 开发进度

- [x] 项目骨架
- [x] 健康检查接口
- [x] 物品接口（假数据）
- [x] 换物接口（假数据）
- [x] 订单接口（假数据）
- [ ] 数据库接入（MySQL + MyBatis-Plus）
- [ ] 用户登录
- [ ] 发布物品
- [ ] 换物流程
- [ ] 二手下单
