package com.myshop.service;

import com.myshop.model.ProductVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品业务逻辑。当前返回内存假数据，后续接入 MyBatis-Plus + MySQL。
 */
@Service
public class ProductService {

    private static final List<ProductVO> MOCK_PRODUCTS = List.of(
            new ProductVO(1L, "全新机械键盘 87 键", new BigDecimal("399.00"), "NEW"),
            new ProductVO(2L, "九成新 iPad Air 4", new BigDecimal("2199.00"), "SECOND_HAND"),
            new ProductVO(3L, "闲置吉他换相机镜头", new BigDecimal("0.00"), "BARTER"),
            new ProductVO(4L, "全新保温杯 500ml", new BigDecimal("89.90"), "NEW")
    );

    /**
     * 查询全部商品。
     *
     * @return 商品列表
     */
    public List<ProductVO> listAll() {
        return MOCK_PRODUCTS;
    }
}
