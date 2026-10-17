# My Shop Backend 架构与写法指引

> 面向后续开发者的「怎么照着写」文档：先讲清目录与分层，再给出可直接模仿的参考写法，
> 最后列出当前任务的接口。接口细节见 [API.md](./API.md)，进度见 [PROGRESS.md](./PROGRESS.md)。

## 一、项目目录结构

```
src/main/java/com/myshop/
├── MyShopApplication.java            # Spring Boot 启动类（Mapper 靠各自的 @Mapper 注解被扫描）
├── common/                           # 与业务无关的公共设施
│   ├── Result.java                   # 统一返回体 {code, message, data}，成功用 Result.success(...)
│   ├── BusinessException.java        # 业务异常（自带 code），可预期失败一律抛它
│   └── GlobalExceptionHandler.java   # @RestControllerAdvice：参数校验 400、业务异常、兜底 500
├── config/                           # 框架配置与请求级上下文
│   ├── CorsConfig.java               # MVC 层跨域（放行 localhost:5173）
│   ├── SecurityConfig.java           # 访问规则、CORS、BCrypt、401/403 统一 JSON
│   ├── JwtAuthenticationFilter.java  # 解析 Authorization: Bearer <token>，写入 SecurityContext 与 UserContext
│   └── UserContext.java              # ThreadLocal 保存当前 userId / username，请求结束必须 clear()
├── controller/                       # HTTP 层：只做参数接收、调用 Service、包 Result，不写业务规则
│   ├── HealthController.java         # GET /api/health
│   ├── AuthController.java           # 注册 / 登录 / 当前用户
│   ├── ItemController.java           # 物品列表与详情
│   ├── BarterController.java         # 换物申请（列表 + 详情 + 发起 + 同意/拒绝）
│   └── OrderController.java          # 二手订单列表
├── entity/                           # 数据库表实体，字段与表结构一一对应，仅供 Mapper 与 Service 使用
│   ├── User.java                     # users
│   ├── Item.java                     # items（condition 是保留字，用 @TableField 转义）
│   ├── BarterRequest.java            # barter_requests
│   └── Order.java                    # orders
├── mapper/                           # 数据访问层：继承 MyBatis-Plus BaseMapper，只有接口没有实现
│   ├── UserMapper.java
│   ├── ItemMapper.java
│   ├── BarterRequestMapper.java
│   └── OrderMapper.java
├── model/                            # 对外数据契约（请求体 / 视图对象），不直接暴露 entity
│   ├── *Request.java                 # 请求体，带 Jakarta Validation 注解
│   ├── *VO.java                      # 视图对象，时间已格式化成字符串、不含敏感字段
│   └── LoginResponse.java            # 组合型响应（token + user）
├── service/                          # 业务层：校验、鉴权、事务、entity ↔ VO 转换都在这里
│   ├── AuthService.java              # 注册 / 登录 / 当前用户（写操作参考）
│   ├── ItemService.java              # 物品查询，JSON 字段与 List 互转
│   ├── BarterService.java            # 换物申请：list / create / getById / accept / reject
│   └── OrderService.java             # 二手订单查询
└── util/
    └── JwtUtil.java                  # JWT 签发与校验（jjwt 0.12.x）

src/main/resources/
├── application.yml                   # 数据源、SQL 初始化、MyBatis-Plus、JWT 配置
├── schema.sql                        # 建表脚本，启动自动执行，必须幂等（CREATE TABLE IF NOT EXISTS）
└── data.sql                          # 初始数据，启动自动执行，必须幂等（INSERT IGNORE + 固定主键）
```

### 分层职责与数据流

```
HTTP 请求
  → JwtAuthenticationFilter  解析 token，写入 UserContext（ThreadLocal）
  → Controller               收参数（@Valid 校验）、调 Service、包 Result<T>
  → Service                  取当前用户、查/改数据、业务规则校验、entity → VO
  → Mapper (BaseMapper)      MyBatis-Plus 通用 CRUD，SQL 由实体注解推导
  → MySQL
异常路径：Service 抛 BusinessException / 校验失败
  → GlobalExceptionHandler   转成 {code, message, data}
```

- **entity 不出 Service**：Controller 只返回 VO，避免把密码、内部字段、`LocalDateTime` 直接暴露给前端。
- **鉴权分两层**：`SecurityConfig` 负责「有没有登录」，Service 负责「能不能动这条数据」（归属校验）。
- **相对时间字段**：VO 里的时间统一格式化为 `yyyy-MM-dd HH:mm:ss` 字符串。

## 二、参考写法指引

新增一个写接口时，按下面顺序照抄即可。

### Service 参考：`AuthService.java`

- 类上 `@Service` + `@RequiredArgsConstructor`，依赖用 `private final` 字段注入（不用 `@Autowired`）。
- 写库方法加 `@Transactional`（`org.springframework.transaction.annotation.Transactional`）。
- 可预期的失败抛 `BusinessException(code, message)`，不要返回 null 让上层猜：
  `throw new BusinessException(401, "未登录")`、`throw new BusinessException(400, "物品不可换")`。
- 查数据用 MyBatis-Plus 的 `LambdaQueryWrapper`，用方法引用写字段名，避免拼错列名：
  ```java
  LambdaQueryWrapper<BarterRequest> wrapper = new LambdaQueryWrapper<>();
  wrapper.eq(BarterRequest::getStatus, "PENDING").orderByAsc(BarterRequest::getId);
  ```
- 实体转 VO 用私有方法 `toVO(entity)`，一处集中处理，不散落在 Controller。

### Controller 参考：`AuthController.java`

```java
@RestController
@RequestMapping("/api/barters")
@RequiredArgsConstructor
public class BarterController {

    private final BarterService barterService;

    @PostMapping
    public Result<BarterRequestVO> create(@Valid @RequestBody CreateBarterRequest req) {
        return Result.success(barterService.create(req));
    }
}
```

- 类上 `@RestController` + `@RequestMapping("/api/xxx")` + `@RequiredArgsConstructor`。
- 入参：`@Valid @RequestBody XxxRequest req`，校验失败由 `GlobalExceptionHandler` 统一转 400。
- 返回：一律 `Result<T>`，成功用 `Result.success(data)`，不要在 Controller 里 catch 业务异常。

### 统一返回 `Result<T>`

```json
{ "code": 200, "message": "success", "data": {} }
```

| 方法 | 用途 |
| --- | --- |
| `Result.success(data)` | 成功，`code=200` |
| `Result.success()` | 成功且无数据 |
| `Result.error(code, message)` | 失败，`code` 与 `BusinessException` 保持一致 |

业务码约定：`200` 成功 / `400` 参数或业务失败 / `401` 未登录 / `403` 越权 / `404` 资源不存在 /
`500` 服务器内部错误。

### 当前用户 `UserContext.getCurrentUserId()`

- 请求进入时由 `JwtAuthenticationFilter` 写入，请求结束时清理；Service 里随取随用。
- **身份只从这里拿，绝不读请求体里的 `userId` / `ownerId`**，这是防越权的根本。
- 返回 `Long`，可能为 `null`（理论上被 Security 拦住，Service 仍做兜底）：
  ```java
  Long fromUserId = UserContext.getCurrentUserId();
  if (fromUserId == null) {
      throw new BusinessException(401, "请先登录");
  }
  ```
- 归属校验用 `equals` 比较包装类型，不要用 `==`：
  ```java
  if (!fromUserId.equals(fromItem.getOwnerId())) {
      throw new BusinessException(403, "只能用自己的物品发起换物");
  }
  // 处理换物申请：只认接收方，发起方不能替对方同意 / 拒绝
  if (!userId.equals(request.getToUserId())) {
      throw new BusinessException(403, "只有接收方能处理该申请");
  }
  ```

### 业务异常 `BusinessException`

- `new BusinessException("消息")` → code 默认 `400`。
- `new BusinessException(code, "消息")` → 指定状态码。
- 抛出后由 `GlobalExceptionHandler` 转成 `Result.error(code, message)`，**HTTP 状态码仍为 200**；
  只有 Spring Security 在 Controller 之前拦截的未登录 / 越权才会带 HTTP 401 / 403。

### 常见坑

- `items.condition` 是 MySQL 保留字：实体已用 `@TableField("`condition`")` 转义，写原生 SQL 记得加反引号。
- `schema.sql` / `data.sql` 每次启动都执行，新增语句必须幂等，否则第二次启动直接失败。
- `UserContext` 用 `ThreadLocal`，过滤器里必须 `finally { UserContext.clear(); }`，否则线程复用会串号。
- 新接口若允许匿名访问，必须显式加进 `SecurityConfig` 的 `permitAll`；否则 `/api/**` 默认要求登录。

## 三、本任务涉及的接口

| 方法 | 路径 | 说明 | 登录 | 实现位置 |
| --- | --- | --- | --- | --- |
| GET | `/api/barters` | 我的换物列表（`?type=from/to/all`） | 是 | `BarterController#list` → `BarterService#list` |
| POST | `/api/barters` | 发起换物（1:1） | 是 | `BarterController#create` → `BarterService#create` |
| GET | `/api/barters/{id}` | 换物详情（仅当事人） | 是 | `BarterController#detail` → `BarterService#getById` |
| PUT | `/api/barters/{id}/accept` | 接收方同意（`PENDING → ACCEPTED`） | 是 | `BarterController#accept` → `BarterService#accept` |
| PUT | `/api/barters/{id}/reject` | 接收方拒绝（`PENDING → REJECTED`） | 是 | `BarterController#reject` → `BarterService#reject` |

`BarterService#list` 的处理顺序：

1. 从 `UserContext` 取当前用户，为空 → `401 请先登录`
2. 归一化 `type`：空 → `all`，`from` / `to` / `all` 原样（转小写），其他 → `400 无效的 type 参数`
3. 拼 `LambdaQueryWrapper`：`from` → `from_user_id = 当前用户`；`to` → `to_user_id = 当前用户`；
   `all` → `(from_user_id = ? OR to_user_id = ?)`（同一行只返回一次，无需额外去重）
4. `orderByDesc(created_at)` 再 `orderByDesc(id)`，最新申请在前且顺序稳定
5. `selectList` → `toVO` 逐条转换，时间格式化成字符串返回

请求体 `CreateBarterRequest`：`toItemId`（想换的，必填）、`fromItemId`（自己提供的，必填）、
`message`（选填，≤500 字）。

`BarterService#create` 的处理顺序：

1. 从 `UserContext` 取当前用户，为空 → `401 请先登录`
2. 校验 `toItem`：不存在 → `404 物品不存在`；状态非 `AVAILABLE` → `400 物品不可换`；
   属于自己 → `400 不能和自己换`
3. 校验 `fromItem`：不存在 → `404 物品不存在`；状态非 `AVAILABLE` → `400 物品不可换`；
   `ownerId` 不是当前用户 → `403 只能用自己的物品发起换物`
4. 查重：同一对 `fromItemId + toItemId` 已有 `PENDING` → `400 已发起过换物申请，请等待对方处理`
5. 组装 `BarterRequest`：`fromUserId` 取登录态、`toUserId` 取 `toItem.ownerId`、
   `priceDiff = 0`、`status = PENDING`、`createdAt = now()`，`insert` 后主键自动回填
6. `toVO(entity)` 返回

`BarterService#getById` 的处理顺序：

1. 从 `UserContext` 取当前用户，为空 → `401 请先登录`
2. `id` 为空或 `selectById` 查不到 → `404 换物申请不存在`
3. 归属校验：`userId` 既不等于 `fromUserId` 也不等于 `toUserId` → `403 无权查看此换物申请`
4. `toVO(entity)` 返回，实体不出 Service

`BarterService#accept` / `#reject` 共用私有方法 `updateStatus(id, targetStatus)`：

1. 从 `UserContext` 取当前用户，为空 → `401 请先登录`
2. 查不到 → `404 换物申请不存在`
3. 归属校验：`userId` 不等于 `toUserId` → `403 只有接收方能处理该申请`（发起方也不能替对方点头）
4. 状态校验：`status` 不是 `PENDING` → `400 该申请已被处理`
5. 并发兜底：用 `LambdaUpdateWrapper` 把 `status = PENDING` 也写进 `WHERE`，
   受影响行数为 0 说明查询后被别人改掉了，同样返回 `400 该申请已被处理`
6. `ACCEPTED` 时把 `fromItemId`、`toItemId` 两件物品定向更新为 `LOCKED`（只 `SET status`，不覆盖其它列）；
   `REJECTED` 不动物品
7. 把内存实体的 `status` 同步为目标值后 `toVO` 返回；方法标 `@Transactional`，状态与物品锁定同生共死

请求 / 响应示例与完整错误码见 [API.md](./API.md)。
