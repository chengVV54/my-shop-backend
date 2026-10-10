package com.myshop.controller;

import com.myshop.common.Result;
import com.myshop.model.OrderVO;
import com.myshop.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 二手订单接口（当前为假数据）。
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 二手订单列表。
     *
     * @return 订单列表
     */
    @GetMapping
    public Result<List<OrderVO>> list() {
        return Result.success(orderService.listAll());
    }
}
