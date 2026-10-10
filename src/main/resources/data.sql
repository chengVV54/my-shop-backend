-- My Shop 初始化数据
-- 由 spring.sql.init.data-locations 在应用启动时自动执行。
-- 使用 INSERT IGNORE + 固定主键，保证重复启动不会重复插入。
-- List 字段（images / want_categories）以 JSON 字符串形式存储。

INSERT IGNORE INTO items
  (id, title, images, description, category, `condition`, owner_id, mode, price,
   want_categories, want_description, allow_price_diff, status, created_at)
VALUES
  (1, '佳能 EOS R6 换平板', '[]',
   '自用佳能 EOS R6 机身，快门数不到 1 万，成色良好，功能一切正常。想换一台平板电脑，iPad 或安卓旗舰平板都可以聊。',
   'CAMERA', 'GOOD', 1, 'BARTER', NULL,
   '["PHONE","COMPUTER"]', '想换 iPad 或 MacBook', 1, 'AVAILABLE', '2026-10-10 17:42:18'),

  (2, '三体全集 换 无线耳机', '[]',
   '《三体》全集三册，重庆出版社版本，只翻阅过一次，书页干净无笔记。想换一副蓝牙耳机，差价可以商量。',
   'BOOK', 'LIKE_NEW', 2, 'BARTER', NULL,
   '["OTHER"]', '想换蓝牙耳机，可补差价', 1, 'AVAILABLE', '2026-10-10 17:42:18'),

  (3, 'iPhone 13 128G 九成新', '[]',
   'iPhone 13 128G 国行版本，电池健康度 89%，屏幕无划痕，一直带壳贴膜使用，配件有原装数据线。',
   'PHONE', 'GOOD', 1, 'SALE', 3000.00,
   NULL, NULL, NULL, 'AVAILABLE', '2026-10-10 17:42:18'),

  (4, '雅马哈民谣吉他', '[]',
   '雅马哈 F310 民谣吉他，41 寸，入手两年，琴颈平直手感好，面板有正常使用痕迹，附赠琴包和拨片。',
   'INSTRUMENT', 'FAIR', 2, 'SALE', 800.00,
   NULL, NULL, NULL, 'AVAILABLE', '2026-10-10 17:42:18'),

  (5, 'Switch OLED 可卖可换', '[]',
   'Switch OLED 白色港版，屏幕完好无坏点，Joy-Con 无漂移，带底座和原装充电器。可以直接买，也可以换手机或电脑。',
   'GAME', 'LIKE_NEW', 1, 'BOTH', 1500.00,
   '["PHONE","COMPUTER"]', '也可以换手机或笔记本电脑', 1, 'AVAILABLE', '2026-10-10 17:42:18'),

  (6, '机械键盘 87 键 可卖可换', '[]',
   '87 键机械键盘，红轴，全新未拆封，带原装键帽和说明书。可低价出，也想换书或游戏卡带，不支持补差价。',
   'COMPUTER', 'NEW', 2, 'BOTH', 300.00,
   '["BOOK","GAME"]', '想换书或游戏卡带，不补差价', 0, 'AVAILABLE', '2026-10-10 17:42:18');

INSERT IGNORE INTO barter_requests
  (id, from_user_id, to_user_id, from_item_id, to_item_id, message, price_diff, status, created_at)
VALUES
  (1, 2, 1, 6, 1, '想用全新机械键盘换你的相机，我补 500 差价，可以详聊。',
   500.00, 'PENDING', '2026-10-10 17:42:18'),

  (2, 1, 2, 5, 2, '用 Switch OLED 换你的三体全集，不用补差价，你看行吗？',
   0.00, 'ACCEPTED', '2026-10-10 17:42:18');

INSERT IGNORE INTO orders
  (id, buyer_id, seller_id, item_id, price, address, status, created_at)
VALUES
  (1, 2, 1, 3, 3000.00, '北京市朝阳区建国路 88 号 3 号楼 1201', 'PAID', '2026-10-10 17:42:18'),

  (2, 1, 2, 4, 800.00, '上海市浦东新区世纪大道 100 号 5 栋 802', 'SHIPPED', '2026-10-10 17:42:18');
