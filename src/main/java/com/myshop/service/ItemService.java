package com.myshop.service;

import com.myshop.model.ItemVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 物品业务逻辑。当前返回内存假数据，后续接入 MyBatis-Plus + MySQL。
 *
 * <p>假数据覆盖三种交易方式：{@code BARTER}（只换）、{@code SALE}（只卖）、{@code BOTH}（可卖可换）。
 */
@Service
public class ItemService {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.CHINA);

    private static final List<ItemVO> MOCK_ITEMS = buildMockItems();

    /**
     * 查询物品列表，可按交易方式过滤。
     *
     * @param mode 交易方式：BARTER / SALE / BOTH；为空（null、空串、空白）时返回全部
     * @return 物品列表
     */
    public List<ItemVO> listAll(String mode) {
        if (mode == null || mode.isBlank()) {
            return new ArrayList<>(MOCK_ITEMS);
        }
        String expected = mode.trim().toUpperCase(Locale.ROOT);
        List<ItemVO> result = new ArrayList<>();
        for (ItemVO item : MOCK_ITEMS) {
            if (expected.equals(item.getMode())) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 根据 ID 查询物品详情。
     *
     * @param id 物品 ID
     * @return 物品对象；不存在或 id 为空时返回 null
     */
    public ItemVO getById(Long id) {
        if (id == null) {
            return null;
        }
        for (ItemVO item : MOCK_ITEMS) {
            if (id.equals(item.getId())) {
                return item;
            }
        }
        return null;
    }

    /**
     * 构建 6 条内存假数据：2 条换物、2 条二手、2 条可卖可换。
     */
    private static List<ItemVO> buildMockItems() {
        String now = LocalDateTime.now().format(TIME_FORMATTER);

        // 1. BARTER：相机换平板
        ItemVO camera = new ItemVO(
                1L,
                "佳能 EOS R6 换平板",
                List.of(),
                "自用佳能 EOS R6 机身，快门数不到 1 万，成色良好，功能一切正常。想换一台平板电脑，iPad 或安卓旗舰平板都可以聊。",
                "CAMERA",
                "GOOD",
                1L,
                "BARTER",
                null,
                List.of("PHONE", "COMPUTER"),
                "想换 iPad 或 MacBook",
                true,
                "AVAILABLE",
                now);

        // 2. BARTER：书换耳机
        ItemVO books = new ItemVO(
                2L,
                "三体全集 换 无线耳机",
                List.of(),
                "《三体》全集三册，重庆出版社版本，只翻阅过一次，书页干净无笔记。想换一副蓝牙耳机，差价可以商量。",
                "BOOK",
                "LIKE_NEW",
                2L,
                "BARTER",
                null,
                List.of("OTHER"),
                "想换蓝牙耳机，可补差价",
                true,
                "AVAILABLE",
                now);

        // 3. SALE：二手 iPhone
        ItemVO iphone = new ItemVO(
                3L,
                "iPhone 13 128G 九成新",
                List.of(),
                "iPhone 13 128G 国行版本，电池健康度 89%，屏幕无划痕，一直带壳贴膜使用，配件有原装数据线。",
                "PHONE",
                "GOOD",
                1L,
                "SALE",
                new BigDecimal("3000.00"),
                null,
                null,
                null,
                "AVAILABLE",
                now);

        // 4. SALE：二手吉他
        ItemVO guitar = new ItemVO(
                4L,
                "雅马哈民谣吉他",
                List.of(),
                "雅马哈 F310 民谣吉他，41 寸，入手两年，琴颈平直手感好，面板有正常使用痕迹，附赠琴包和拨片。",
                "INSTRUMENT",
                "FAIR",
                2L,
                "SALE",
                new BigDecimal("800.00"),
                null,
                null,
                null,
                "AVAILABLE",
                now);

        // 5. BOTH：游戏机
        ItemVO gameConsole = new ItemVO(
                5L,
                "Switch OLED 可卖可换",
                List.of(),
                "Switch OLED 白色港版，屏幕完好无坏点，Joy-Con 无漂移，带底座和原装充电器。可以直接买，也可以换手机或电脑。",
                "GAME",
                "LIKE_NEW",
                1L,
                "BOTH",
                new BigDecimal("1500.00"),
                List.of("PHONE", "COMPUTER"),
                "也可以换手机或笔记本电脑",
                true,
                "AVAILABLE",
                now);

        // 6. BOTH：键盘
        ItemVO keyboard = new ItemVO(
                6L,
                "机械键盘 87 键 可卖可换",
                List.of(),
                "87 键机械键盘，红轴，全新未拆封，带原装键帽和说明书。可低价出，也想换书或游戏卡带，不支持补差价。",
                "COMPUTER",
                "NEW",
                2L,
                "BOTH",
                new BigDecimal("300.00"),
                List.of("BOOK", "GAME"),
                "想换书或游戏卡带，不补差价",
                false,
                "AVAILABLE",
                now);

        return List.of(camera, books, iphone, guitar, gameConsole, keyboard);
    }
}
