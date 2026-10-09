# My Shop Backend

个人电商项目后端，支持**新品**、**二手**、**以物换物**三种交易模式。

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
| GET | `/api/products` | 商品列表（假数据） |

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
│   └── ProductController.java      # 商品接口
├── model/
│   └── ProductVO.java              # 商品视图对象
└── service/
    └── ProductService.java         # 商品业务逻辑（当前为假数据）
```

## 配套前端仓库

<https://github.com/chengVV54/my-shop-frontend>

## 开发进度清单

- [x] 项目骨架
- [x] 健康检查接口
- [x] 商品列表接口（假数据）
- [ ] 数据库接入（MySQL + MyBatis-Plus）
- [ ] 用户登录
- [ ] 商品 CRUD
- [ ] 二手交易
- [ ] 以物换物
