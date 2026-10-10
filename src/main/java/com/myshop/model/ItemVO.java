package com.myshop.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 物品视图对象。
 *
 * <p>物品同时支持「换物」与「二手」两种交易方式，通过 {@code mode} 区分：
 * <ul>
 *     <li>{@code BARTER}：只换不卖，此时使用 {@code wantCategories} / {@code wantDescription} / {@code allowPriceDiff}</li>
 *     <li>{@code SALE}：只卖不换，此时使用 {@code price}</li>
 *     <li>{@code BOTH}：可卖可换，{@code price} 与换物相关字段都有值</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemVO {

    /** 物品 ID */
    private Long id;

    /** 物品标题 */
    private String title;

    /** 物品图片地址列表 */
    private List<String> images;

    /** 物品详细描述 */
    private String description;

    /** 物品分类：CAMERA / BOOK / INSTRUMENT / GAME / PHONE / COMPUTER / CLOTHING / OTHER */
    private String category;

    /** 物品成色：NEW / LIKE_NEW / GOOD / FAIR / POOR */
    private String condition;

    /** 发布者用户 ID */
    private Long ownerId;

    /** 交易方式：BARTER 换物 / SALE 二手 / BOTH 都可 */
    private String mode;

    /** 售价，仅当 mode 为 SALE 或 BOTH 时有值，其余情况为 null */
    private BigDecimal price;

    /** 想换取的物品分类，仅当 mode 为 BARTER 或 BOTH 时有值 */
    private List<String> wantCategories;

    /** 想换什么的补充描述 */
    private String wantDescription;

    /** 是否接受补差价 */
    private Boolean allowPriceDiff;

    /** 物品状态：AVAILABLE / LOCKED / DONE / REMOVED */
    private String status;

    /** 创建时间（字符串形式） */
    private String createdAt;
}
