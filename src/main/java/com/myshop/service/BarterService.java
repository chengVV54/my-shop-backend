package com.myshop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.myshop.common.BusinessException;
import com.myshop.config.UserContext;
import com.myshop.entity.BarterRequest;
import com.myshop.entity.Item;
import com.myshop.mapper.BarterRequestMapper;
import com.myshop.mapper.ItemMapper;
import com.myshop.model.BarterRequestVO;
import com.myshop.model.CreateBarterRequest;
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
 * 换物申请业务逻辑：数据来自 MySQL 的 {@code barter_requests} 表，对外返回 {@link BarterRequestVO} 不变。
 *
 * <p>安全约定（与 {@code AuthService} 一致）：
 * <ul>
 *     <li>当前用户身份只从 {@link UserContext} 取，忽略前端传来的任何用户 ID</li>
 *     <li>查询只返回与当前用户相关的数据（我发起的 / 我收到的），绝不返回全表</li>
 *     <li>写操作时参与换物的物品必须存在、状态为 {@code AVAILABLE}，且我方物品必须属于当前用户</li>
 *     <li>失败一律抛 {@link BusinessException}，由全局异常处理转成统一返回体</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class BarterService {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.CHINA);

    /** 新申请的初始状态：等待对方处理 */
    private static final String STATUS_PENDING = "PENDING";

    /** 物品唯一可参与换物的状态，LOCKED / DONE / REMOVED 都不允许再发起 */
    private static final String ITEM_STATUS_AVAILABLE = "AVAILABLE";

    /** 列表过滤方向：我发起的 */
    private static final String TYPE_FROM = "from";

    /** 列表过滤方向：我收到的 */
    private static final String TYPE_TO = "to";

    /** 列表过滤方向：全部（我发起的 + 我收到的） */
    private static final String TYPE_ALL = "all";

    private final BarterRequestMapper barterRequestMapper;

    private final ItemMapper itemMapper;

    /**
     * 发起换物申请（1:1）。
     *
     * <p>校验顺序：登录 → 目标物品（存在 / 可换 / 不是自己的）→ 我方物品（存在 / 可换 / 属于自己）
     * → 重复申请，全部通过后才落库；任一步失败都直接抛业务异常，不会产生半条数据。
     *
     * @param req 换物请求体（两个物品 ID + 可选留言），非空字段已由 {@code @Valid} 保证
     * @return 新建的换物申请（含数据库自增 ID 与创建时间）
     * @throws BusinessException 未登录 401、物品不存在 404、物品不可换或重复申请 400、物品不属于自己 403
     */
    @Transactional
    public BarterRequestVO create(CreateBarterRequest req) {
        // 1. 身份只认 JWT 解析结果：ThreadLocal 里的用户由 JwtAuthenticationFilter 写入
        Long fromUserId = UserContext.getCurrentUserId();
        if (fromUserId == null) {
            throw new BusinessException(401, "请先登录");
        }

        // 2. 校验想换的物品：必须存在、可换，且不属于自己（自己换自己没有意义）
        Item toItem = itemMapper.selectById(req.getToItemId());
        if (toItem == null) {
            throw new BusinessException(404, "物品不存在");
        }
        if (!ITEM_STATUS_AVAILABLE.equals(toItem.getStatus())) {
            throw new BusinessException(400, "物品不可换");
        }
        if (fromUserId.equals(toItem.getOwnerId())) {
            throw new BusinessException(400, "不能和自己换");
        }

        // 3. 校验我方物品：必须存在、可换，且 owner 就是当前用户（否则等于拿别人的东西去换）
        Item fromItem = itemMapper.selectById(req.getFromItemId());
        if (fromItem == null) {
            throw new BusinessException(404, "物品不存在");
        }
        if (!ITEM_STATUS_AVAILABLE.equals(fromItem.getStatus())) {
            throw new BusinessException(400, "物品不可换");
        }
        if (!fromUserId.equals(fromItem.getOwnerId())) {
            throw new BusinessException(403, "只能用自己的物品发起换物");
        }

        // 4. 同一对物品之间只允许存在一条 PENDING 申请，避免刷屏骚扰对方
        LambdaQueryWrapper<BarterRequest> duplicateWrapper = new LambdaQueryWrapper<>();
        duplicateWrapper.eq(BarterRequest::getFromItemId, req.getFromItemId())
                .eq(BarterRequest::getToItemId, req.getToItemId())
                .eq(BarterRequest::getStatus, STATUS_PENDING);
        if (barterRequestMapper.selectCount(duplicateWrapper) > 0) {
            throw new BusinessException(400, "已发起过换物申请，请等待对方处理");
        }

        // 5. 组装实体：发起方 = 当前用户，接收方从目标物品的 ownerId 推导，两者都不接受前端传值
        BarterRequest entity = new BarterRequest();
        entity.setFromUserId(fromUserId);
        entity.setToUserId(toItem.getOwnerId());
        entity.setFromItemId(req.getFromItemId());
        entity.setToItemId(req.getToItemId());
        entity.setMessage(req.getMessage());
        // 最小版本不计算差价，先固定为 0（对应「不涉及差价」的业务含义）
        entity.setPriceDiff(BigDecimal.ZERO);
        entity.setStatus(STATUS_PENDING);
        entity.setCreatedAt(LocalDateTime.now());
        barterRequestMapper.insert(entity);

        // 6. insert 会把自增主键回填进 entity.id，因此可以直接转 VO 返回给前端
        return toVO(entity);
    }

    /**
     * 查询「我的换物列表」：只返回与当前登录用户相关的换物申请。
     *
     * <p>过滤方向由 {@code type} 决定：{@code from} 我发起的、{@code to} 我收到的、
     * {@code all}（含不传、空串）两者合并。身份取自 {@link UserContext}，
     * 前端无法通过参数查询别人的换物申请。
     *
     * @param type 过滤方向：{@code from} / {@code to} / {@code all}，大小写不敏感
     * @return 换物申请列表，按创建时间倒序（同一时刻再按 id 倒序，保证顺序稳定）
     * @throws BusinessException 未登录 401、type 取值非法 400
     */
    public List<BarterRequestVO> list(String type) {
        // 1. 身份只认登录态；Security 已拦截未登录请求，这里是 Service 层的兜底
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }

        // 2. 先把 type 归一化成 from / to / all，非法值在这里被拒绝，避免拼接出奇怪的条件
        String direction = normalizeType(type);

        // 3. 按方向拼查询条件：只查当前用户参与的数据
        LambdaQueryWrapper<BarterRequest> wrapper = new LambdaQueryWrapper<>();
        switch (direction) {
            case TYPE_FROM -> wrapper.eq(BarterRequest::getFromUserId, userId);
            case TYPE_TO -> wrapper.eq(BarterRequest::getToUserId, userId);
            default -> wrapper.and(w -> w.eq(BarterRequest::getFromUserId, userId)
                    .or()
                    .eq(BarterRequest::getToUserId, userId));
        }

        // 4. 最新申请排在前面；created_at 可能相同，追加 id 倒序让结果稳定可预期
        wrapper.orderByDesc(BarterRequest::getCreatedAt)
                .orderByDesc(BarterRequest::getId);

        // 5. 查库并逐条转 VO，时间在 toVO 里统一格式化
        return barterRequestMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    /**
     * 归一化列表过滤方向。
     *
     * @param type 前端传入的原始值，可为 null / 空串
     * @return 恒为 {@link #TYPE_FROM}、{@link #TYPE_TO}、{@link #TYPE_ALL} 之一
     * @throws BusinessException 传入其他值时抛 400
     */
    private String normalizeType(String type) {
        if (!StringUtils.hasText(type)) {
            return TYPE_ALL;
        }
        String normalized = type.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case TYPE_FROM, TYPE_TO, TYPE_ALL -> normalized;
            default -> throw new BusinessException(400, "无效的 type 参数");
        };
    }

    /**
     * 实体转视图对象：时间格式化成字符串。
     */
    private BarterRequestVO toVO(BarterRequest entity) {
        BarterRequestVO vo = new BarterRequestVO();
        vo.setId(entity.getId());
        vo.setFromUserId(entity.getFromUserId());
        vo.setToUserId(entity.getToUserId());
        vo.setFromItemId(entity.getFromItemId());
        vo.setToItemId(entity.getToItemId());
        vo.setMessage(entity.getMessage());
        vo.setPriceDiff(entity.getPriceDiff());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(formatTime(entity.getCreatedAt()));
        return vo;
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? null : time.format(TIME_FORMATTER);
    }
}
