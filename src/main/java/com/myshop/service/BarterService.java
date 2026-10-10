package com.myshop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.myshop.entity.BarterRequest;
import com.myshop.mapper.BarterRequestMapper;
import com.myshop.model.BarterRequestVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * 换物申请业务逻辑：数据来自 MySQL 的 {@code barter_requests} 表，对外返回 {@link BarterRequestVO} 不变。
 */
@Service
@RequiredArgsConstructor
public class BarterService {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.CHINA);

    private final BarterRequestMapper barterRequestMapper;

    /**
     * 查询全部换物申请。
     *
     * @return 换物申请列表，按 id 升序
     */
    public List<BarterRequestVO> listAll() {
        LambdaQueryWrapper<BarterRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(BarterRequest::getId);
        return barterRequestMapper.selectList(wrapper).stream().map(this::toVO).toList();
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
