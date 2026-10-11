-- My Shop 建表脚本
-- 由 spring.sql.init.schema-locations 在应用启动时自动执行。
-- 全部使用 CREATE TABLE IF NOT EXISTS，可重复执行。

CREATE TABLE IF NOT EXISTS users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL UNIQUE,
  -- 存 BCrypt 哈希（60 字符），禁止明文
  password VARCHAR(100) NOT NULL,
  avatar VARCHAR(255),
  credit INT DEFAULT 100,
  created_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS items (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(200) NOT NULL,
  images TEXT,
  description TEXT,
  category VARCHAR(50) NOT NULL,
  `condition` VARCHAR(50) NOT NULL,
  owner_id BIGINT NOT NULL,
  mode VARCHAR(20) NOT NULL,
  price DECIMAL(10,2),
  want_categories TEXT,
  want_description VARCHAR(500),
  allow_price_diff TINYINT(1),
  status VARCHAR(20) NOT NULL,
  created_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS barter_requests (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  from_user_id BIGINT NOT NULL,
  to_user_id BIGINT NOT NULL,
  from_item_id BIGINT NOT NULL,
  to_item_id BIGINT NOT NULL,
  message VARCHAR(500),
  price_diff DECIMAL(10,2),
  status VARCHAR(20) NOT NULL,
  created_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS orders (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  buyer_id BIGINT NOT NULL,
  seller_id BIGINT NOT NULL,
  item_id BIGINT NOT NULL,
  price DECIMAL(10,2) NOT NULL,
  address VARCHAR(200),
  status VARCHAR(20) NOT NULL,
  created_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
