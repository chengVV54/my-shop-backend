package com.myshop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myshop.entity.Item;
import com.myshop.mapper.ItemMapper;
import com.myshop.model.ItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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

    private final ItemMapper itemMapper;

    private final ObjectMapper objectMapper;

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
     * 供后续「发布物品」等写入场景使用，与 {@link #toVO(Item)} 互为逆操作。
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
