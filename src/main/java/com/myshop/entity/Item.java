package com.myshop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 物品表实体，对应 {@code items} 表。
 *
 * <p>列表型字段（{@code images}、{@code wantCategories}）在库中以 JSON 字符串形式存储，
 * 由 {@code ItemService} 负责与 {@code List<String>} 之间的转换。
 */
@Data
@TableName("items")
public class Item {

    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 物品标题 */
    private String title;

    /** 物品图片地址列表，JSON 字符串，如 {@code ["a.jpg","b.jpg"]} */
    private String images;

    /** 物品详细描述 */
    private String description;

    /** 物品分类：CAMERA / BOOK / INSTRUMENT / GAME / PHONE / COMPUTER / CLOTHING / OTHER */
    private String category;

    /** 物品成色：NEW / LIKE_NEW / GOOD / FAIR / POOR。condition 是 MySQL 保留字，需转义 */
    @TableField("`condition`")
    private String condition;

    /** 发布者用户 ID */
    private Long ownerId;

    /** 交易方式：BARTER 换物 / SALE 二手 / BOTH 都可 */
    private String mode;

    /** 售价，仅当 mode 为 SALE 或 BOTH 时有值 */
    private BigDecimal price;

    /** 想换取的物品分类，JSON 字符串，仅换物相关模式有值 */
    private String wantCategories;

    /** 想换什么的补充描述 */
    private String wantDescription;

    /** 是否接受补差价 */
    private Boolean allowPriceDiff;

    /** 物品状态：AVAILABLE / LOCKED / DONE / REMOVED */
    private String status;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
