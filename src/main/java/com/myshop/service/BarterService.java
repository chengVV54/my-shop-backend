package com.myshop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
 *     <li>单条详情只对换物双方开放；同意 / 拒绝只对接收方开放，发起方不能替对方点头</li>
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

    /** 接收方同意后的状态 */
    private static final String STATUS_ACCEPTED = "ACCEPTED";

    /** 接收方拒绝后的状态 */
    private static final String STATUS_REJECTED = "REJECTED";

    /** 物品唯一可参与换物的状态，LOCKED / DONE / REMOVED 都不允许再发起 */
    private static final String ITEM_STATUS_AVAILABLE = "AVAILABLE";

    /** 换物达成后物品被锁定，防止同一件物品再被其他人换走 */
    private static final String ITEM_STATUS_LOCKED = "LOCKED";

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
     * 查询单条换物申请详情。
     *
     * <p>详情属于换物双方的私密数据，只有发起方与接收方本人可以查看；
     * 其他登录用户即使知道 id 也拿不到，统一返回 403，避免枚举 id 探测别人的交易。
     *
     * @param id 换物申请 ID
     * @return 换物申请详情
     * @throws BusinessException 未登录 401、申请不存在 404、非当事人 403
     */
    public BarterRequestVO getById(Long id) {
        // 1. 身份只认登录态，不接受前端传用户 ID
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }

        // 2. 查库：id 为空或库里没有这条记录，都按「不存在」处理
        BarterRequest request = findById(id);

        // 3. 归属校验：必须是发起方或接收方之一，否则一律 403（不让局外人知道这条申请的存在与内容）
        if (!userId.equals(request.getFromUserId()) && !userId.equals(request.getToUserId())) {
            throw new BusinessException(403, "无权查看此换物申请");
        }

        // 4. 实体只留在 Service 内，对外统一转 VO
        return toVO(request);
    }

    /**
     * 接收方同意换物申请：{@code PENDING → ACCEPTED}，并把双方物品锁定为 {@code LOCKED}。
     *
     * @param id 换物申请 ID
     * @return 更新后的换物申请
     * @throws BusinessException 未登录 401、申请不存在 404、非接收方 403、已被处理 400
     */
    @Transactional
    public BarterRequestVO accept(Long id) {
        return updateStatus(id, STATUS_ACCEPTED);
    }

    /**
     * 接收方拒绝换物申请：{@code PENDING → REJECTED}，物品保持原状态不变。
     *
     * @param id 换物申请 ID
     * @return 更新后的换物申请
     * @throws BusinessException 未登录 401、申请不存在 404、非接收方 403、已被处理 400
     */
    @Transactional
    public BarterRequestVO reject(Long id) {
        return updateStatus(id, STATUS_REJECTED);
    }

    /**
     * 同意 / 拒绝共用的状态流转逻辑。
     *
     * <p>校验顺序：登录 → 申请存在 → 只有接收方能操作 → 当前状态必须是 {@code PENDING}
     * → 带 {@code status = PENDING} 条件更新（并发兜底）→ 同意时锁定双方物品。
     *
     * @param id           换物申请 ID
     * @param targetStatus 目标状态：{@link #STATUS_ACCEPTED} 或 {@link #STATUS_REJECTED}
     * @return 更新后的换物申请
     * @throws BusinessException 未登录 401、申请不存在 404、非接收方 403、已被处理 400
     */
    private BarterRequestVO updateStatus(Long id, String targetStatus) {
        // 1. 身份只认登录态
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }

        // 2. 查库：不存在直接 404
        BarterRequest request = findById(id);

        // 3. 归属校验：只有「接收方」有权处理，发起方自己不能替对方同意（更不能用别人的 id 操作）
        if (!userId.equals(request.getToUserId())) {
            throw new BusinessException(403, "只有接收方能处理该申请");
        }

        // 4. 状态校验：只有待处理的申请能流转，已同意 / 已拒绝 / 已完成的一律拒绝重复操作
        if (!STATUS_PENDING.equals(request.getStatus())) {
            throw new BusinessException(400, "该申请已被处理");
        }

        // 5. 条件更新：把 status = PENDING 写进 WHERE，两个人同时点「同意」时只有一条能更新成功。
        //    「先查后改」之间永远存在缝隙，这一步是并发兜底（未加唯一索引也能挡住双击 / 双端并发）。
        LambdaUpdateWrapper<BarterRequest> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(BarterRequest::getId, id)
                .eq(BarterRequest::getStatus, STATUS_PENDING)
                .set(BarterRequest::getStatus, targetStatus);
        int updated = barterRequestMapper.update(null, updateWrapper);
        if (updated == 0) {
            // 查询之后、更新之前状态被别人改掉了
            throw new BusinessException(400, "该申请已被处理");
        }

        // 6. 同意后锁定双方物品，避免同一件物品再被其他人发起换物（拒绝则物品状态不动）
        if (STATUS_ACCEPTED.equals(targetStatus)) {
            lockItem(request.getFromItemId());
            lockItem(request.getToItemId());
        }

        // 7. 数据库已更新，把内存里的实体同步成新状态后再转 VO 返回
        request.setStatus(targetStatus);
        return toVO(request);
    }

    /**
     * 按 ID 查换物申请，不存在时抛 404。
     *
     * @param id 换物申请 ID，可能为 null
     * @return 数据库中的换物申请实体
     * @throws BusinessException 不存在时抛 404
     */
    private BarterRequest findById(Long id) {
        if (id == null) {
            throw new BusinessException(404, "换物申请不存在");
        }
        BarterRequest request = barterRequestMapper.selectById(id);
        if (request == null) {
            throw new BusinessException(404, "换物申请不存在");
        }
        return request;
    }

    /**
     * 把物品置为 {@link #ITEM_STATUS_LOCKED}。
     *
     * <p>用 {@code set status} 的定向更新，只改这一列，不会被并发请求用旧值覆盖标题、价格等其它字段。
     *
     * @param itemId 物品 ID，为 null 时直接跳过（正常数据不会为 null，这里只做防御）
     */
    private void lockItem(Long itemId) {
        if (itemId == null) {
            return;
        }
        LambdaUpdateWrapper<Item> lockWrapper = new LambdaUpdateWrapper<>();
        lockWrapper.eq(Item::getId, itemId)
                .set(Item::getStatus, ITEM_STATUS_LOCKED);
        itemMapper.update(null, lockWrapper);
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
