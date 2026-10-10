package com.myshop.service;

import com.myshop.model.OrderVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 二手订单业务逻辑。当前返回内存假数据，后续接入 MyBatis-Plus + MySQL。
 */
@Service
public class OrderService {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.CHINA);

    private static final List<OrderVO> MOCK_ORDERS = buildMockOrders();

    /**
     * 查询全部二手订单。
     *
     * @return 订单列表
     */
    public List<OrderVO> listAll() {
        return new ArrayList<>(MOCK_ORDERS);
    }

    /**
     * 构建 2 条内存假数据。
     */
    private static List<OrderVO> buildMockOrders() {
        String now = LocalDateTime.now().format(TIME_FORMATTER);

        // 用户 2 买下用户 1 的「iPhone 13」（物品 3），已付款
        OrderVO order1 = new OrderVO(
                1L,
                2L,
                1L,
                3L,
                new BigDecimal("3000.00"),
                "北京市朝阳区建国路 88 号 3 号楼 1201",
                "PAID",
                now);

        // 用户 1 买下用户 2 的「雅马哈民谣吉他」（物品 4），已发货
        OrderVO order2 = new OrderVO(
                2L,
                1L,
                2L,
                4L,
                new BigDecimal("800.00"),
                "上海市浦东新区世纪大道 100 号 5 栋 802",
                "SHIPPED",
                now);

        return List.of(order1, order2);
    }
}
