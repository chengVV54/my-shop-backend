package com.myshop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.myshop.entity.Order;
import com.myshop.mapper.OrderMapper;
import com.myshop.model.OrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * 二手订单业务逻辑：数据来自 MySQL 的 {@code orders} 表，对外返回 {@link OrderVO} 不变。
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.CHINA);

    private final OrderMapper orderMapper;

    /**
     * 查询全部二手订单。
     *
     * @return 订单列表，按 id 升序
     */
    public List<OrderVO> listAll() {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Order::getId);
        return orderMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    /**
     * 实体转视图对象：时间格式化成字符串。
     */
    private OrderVO toVO(Order entity) {
        OrderVO vo = new OrderVO();
        vo.setId(entity.getId());
        vo.setBuyerId(entity.getBuyerId());
        vo.setSellerId(entity.getSellerId());
        vo.setItemId(entity.getItemId());
        vo.setPrice(entity.getPrice());
        vo.setAddress(entity.getAddress());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(formatTime(entity.getCreatedAt()));
        return vo;
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? null : time.format(TIME_FORMATTER);
    }
}
