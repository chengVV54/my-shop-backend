package com.myshop.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 发布物品请求体。
 *
 * <p>只描述「物品本身」：标题、描述、分类、成色、交易方式、价格与换物偏好。
 * 发布者身份刻意不放在请求体里，由服务端从 JWT 解析出的
 * {@code com.myshop.config.UserContext#getCurrentUserId()} 获取，避免前端伪造 {@code ownerId}。
 *
 * <p>价格与交易方式相关：{@code mode} 为 {@code SALE} / {@code BOTH} 时必须传 {@code price}
 * 且大于 0。字段上的 {@link DecimalMin} 只能约束「传了值」的情况，
 * 「按 mode 判断是否必填」属于跨字段规则，放在 Service 里校验。
 */
@Data
public class CreateItemRequest {

    /** 物品标题，必填，最长 200 字（对应 items.title VARCHAR(200)） */
    @NotBlank(message = "不能为空")
    @Size(max = 200, message = "长度不能超过 200 字")
    private String title;

    /** 物品详细描述，选填（对应 items.description TEXT） */
    private String description;

    /** 物品分类，必填，如 CAMERA / BOOK / INSTRUMENT / GAME / PHONE / COMPUTER / CLOTHING / OTHER */
    @NotBlank(message = "不能为空")
    @Size(max = 50, message = "长度不能超过 50 字")
    private String category;

    /** 物品成色，必填，如 NEW / LIKE_NEW / GOOD / FAIR / POOR */
    @NotBlank(message = "不能为空")
    @Size(max = 50, message = "长度不能超过 50 字")
    private String condition;

    /** 交易方式，必填：BARTER 换物 / SALE 二手 / BOTH 都可（大小写不敏感，Service 会归一化） */
    @NotBlank(message = "不能为空")
    @Pattern(regexp = "(?i)BARTER|SALE|BOTH", message = "取值必须为 BARTER / SALE / BOTH")
    private String mode;

    /**
     * 售价，SALE / BOTH 时必填且必须大于 0；BARTER 时可以为空。
     *
     * <p>{@link DecimalMin} 对 {@code null} 不生效，正好留给 Service 做条件必填校验。
     */
    @DecimalMin(value = "0.0", inclusive = false, message = "必须大于 0")
    private BigDecimal price;

    /** 想换取的物品分类列表，BARTER / BOTH 时选填，库中以 JSON 字符串存储 */
    private List<String> wantCategories;

    /** 想换什么的补充描述，选填，最长 500 字（对应 items.want_description VARCHAR(500)） */
    @Size(max = 500, message = "长度不能超过 500 字")
    private String wantDescription;

    /** 是否接受补差价，选填 */
    private Boolean allowPriceDiff;
}
