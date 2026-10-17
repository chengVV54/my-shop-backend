package com.myshop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myshop.common.BusinessException;
import com.myshop.config.UserContext;
import com.myshop.entity.Item;
import com.myshop.mapper.ItemMapper;
import com.myshop.model.CreateItemRequest;
import com.myshop.model.ItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * 物品业务逻辑：数据来自 MySQL 的 {@code items} 表，对外返回 {@link ItemVO} 不变。
 *
 * <p>{@code images} 与 {@code want_categories} 在库中以 JSON 字符串存储，
 * 这里负责与 {@code List<String>} 互转。
 */
@Service
@RequiredArgsConstructor
public class ItemService {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.CHINA);

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    /** 新发布物品的初始状态：可交易 */
    private static final String STATUS_AVAILABLE = "AVAILABLE";

    /** 交易方式：只换不卖 */
    private static final String MODE_BARTER = "BARTER";

    /** 交易方式：只卖不换 */
    private static final String MODE_SALE = "SALE";

    /** 交易方式：可卖可换 */
    private static final String MODE_BOTH = "BOTH";

    private final ItemMapper itemMapper;

    private final ObjectMapper objectMapper;

    /**
     * 发布物品。
     *
     * <p>发布者身份只取登录态，不接受前端传入的 {@code ownerId}；
     * 价格必填性依赖 {@code mode}，属于跨字段规则，在这里校验。
     *
     * @param req 发布物品请求体，非空字段已由 {@code @Valid} 保证
     * @return 新建的物品（含数据库自增 ID、状态与创建时间）
     * @throws BusinessException 未登录 401、价格缺失 / 非正数 400、mode 非法 400
     */
    @Transactional
    public ItemVO create(CreateItemRequest req) {
        // 1. 身份只认 JWT 解析结果：ThreadLocal 里的用户由 JwtAuthenticationFilter 写入
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }

        // 2. 归一化 mode（大小写不敏感），并做「SALE / BOTH 时 price 必填且 > 0」的条件校验
        String mode = normalizeMode(req.getMode());
        validatePrice(mode, req.getPrice());

        // 3. 组装实体：ownerId / status / createdAt 全部由服务端决定，前端无法伪造
        Item item = new Item();
        item.setTitle(req.getTitle().trim());
        item.setDescription(req.getDescription());
        item.setCategory(req.getCategory().trim());
        item.setCondition(req.getCondition().trim());
        item.setOwnerId(userId);
        item.setMode(mode);
        item.setPrice(req.getPrice());
        // 列表字段在库中是 JSON 字符串，复用已有的序列化方法；为 null 时保持 NULL
        item.setWantCategories(writeJsonList(req.getWantCategories()));
        item.setWantDescription(req.getWantDescription());
        item.setAllowPriceDiff(req.getAllowPriceDiff());
        item.setStatus(STATUS_AVAILABLE);
        item.setCreatedAt(LocalDateTime.now());
        itemMapper.insert(item);

        // 4. insert 会把自增主键回填进 entity.id，直接转 VO 返回，实体不出 Service
        return toVO(item);
    }

    /**
     * 查询物品列表，可按交易方式过滤。
     *
     * @param mode 交易方式：BARTER / SALE / BOTH；为空（null、空串、空白）时返回全部
     * @return 物品列表，按 id 升序
     */
    public List<ItemVO> listAll(String mode) {
        LambdaQueryWrapper<Item> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(mode)) {
            wrapper.eq(Item::getMode, mode.trim().toUpperCase(Locale.ROOT));
        }
        wrapper.orderByAsc(Item::getId);
        return itemMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    /**
     * 根据 ID 查询物品详情。
     *
     * @param id 物品 ID
     * @return 物品对象；库中不存在或 id 为空时返回 null
     */
    public ItemVO getById(Long id) {
        if (id == null) {
            return null;
        }
        Item item = itemMapper.selectById(id);
        return item == null ? null : toVO(item);
    }

    /**
     * 归一化交易方式：去空白并转大写。
     *
     * <p>请求体上的 {@code @Pattern} 已做过一次大小写不敏感的格式校验，
     * 这里再做一次兜底，避免非法值被原样写进库。
     *
     * @param mode 前端传入的原始交易方式
     * @return 恒为 {@link #MODE_BARTER}、{@link #MODE_SALE}、{@link #MODE_BOTH} 之一
     * @throws BusinessException 为空或取值非法时抛 400
     */
    private String normalizeMode(String mode) {
        if (!StringUtils.hasText(mode)) {
            throw new BusinessException(400, "mode 不能为空");
        }
        String normalized = mode.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case MODE_BARTER, MODE_SALE, MODE_BOTH -> normalized;
            default -> throw new BusinessException(400, "无效的 mode 参数");
        };
    }

    /**
     * 按交易方式校验价格：SALE / BOTH 必须传 price 且大于 0，BARTER 不要求。
     *
     * @param mode  归一化后的交易方式
     * @param price 售价，可能为 null
     * @throws BusinessException 价格缺失或非正数时抛 400
     */
    private void validatePrice(String mode, BigDecimal price) {
        if (!MODE_SALE.equals(mode) && !MODE_BOTH.equals(mode)) {
            return;
        }
        if (price == null) {
            throw new BusinessException(400, "mode 为 SALE 或 BOTH 时 price 不能为空");
        }
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "price 必须大于 0");
        }
    }

    /**
     * 实体转视图对象：JSON 字符串字段还原成列表，时间格式化成字符串。
     */
    private ItemVO toVO(Item item) {
        ItemVO vo = new ItemVO();
        vo.setId(item.getId());
        vo.setTitle(item.getTitle());
        // images 语义上永远是列表：库中为 NULL 时按空列表返回，避免前端遍历报错
        List<String> images = parseJsonList(item.getImages());
        vo.setImages(images == null ? List.of() : images);
        vo.setDescription(item.getDescription());
        vo.setCategory(item.getCategory());
        vo.setCondition(item.getCondition());
        vo.setOwnerId(item.getOwnerId());
        vo.setMode(item.getMode());
        vo.setPrice(item.getPrice());
        // wantCategories 仅在换物相关模式下有意义，为空时保持 null 以区分「不适用」
        vo.setWantCategories(parseJsonList(item.getWantCategories()));
        vo.setWantDescription(item.getWantDescription());
        vo.setAllowPriceDiff(item.getAllowPriceDiff());
        vo.setStatus(item.getStatus());
        vo.setCreatedAt(formatTime(item.getCreatedAt()));
        return vo;
    }

    /**
     * 视图对象转实体：列表序列化成 JSON 字符串，时间字符串解析成 {@link LocalDateTime}。
     * 供后续「编辑物品」等写入场景使用，与 {@link #toVO(Item)} 互为逆操作。
     */
    private Item toEntity(ItemVO vo) {
        if (vo == null) {
            return null;
        }
        Item item = new Item();
        item.setId(vo.getId());
        item.setTitle(vo.getTitle());
        item.setImages(writeJsonList(vo.getImages() == null ? List.of() : vo.getImages()));
        item.setDescription(vo.getDescription());
        item.setCategory(vo.getCategory());
        item.setCondition(vo.getCondition());
        item.setOwnerId(vo.getOwnerId());
        item.setMode(vo.getMode());
        item.setPrice(vo.getPrice());
        item.setWantCategories(writeJsonList(vo.getWantCategories()));
        item.setWantDescription(vo.getWantDescription());
        item.setAllowPriceDiff(vo.getAllowPriceDiff());
        item.setStatus(vo.getStatus());
        item.setCreatedAt(parseTime(vo.getCreatedAt()));
        return item;
    }

    /**
     * JSON 字符串转列表。
     *
     * @return 入参为空时返回 null；内容损坏时抛出异常以便尽早发现脏数据
     */
    private List<String> parseJsonList(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, STRING_LIST_TYPE);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("物品列表字段 JSON 解析失败: " + json, ex);
        }
    }

    /**
     * 列表转 JSON 字符串。
     *
     * @return 入参为 null 时返回 null
     */
    private String writeJsonList(List<String> values) {
        if (values == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("物品列表字段 JSON 序列化失败: " + values, ex);
        }
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? null : time.format(TIME_FORMATTER);
    }

    private LocalDateTime parseTime(String text) {
        return StringUtils.hasText(text) ? LocalDateTime.parse(text.trim(), TIME_FORMATTER) : null;
    }
}
