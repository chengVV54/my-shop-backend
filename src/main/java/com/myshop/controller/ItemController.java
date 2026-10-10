package com.myshop.controller;

import com.myshop.common.Result;
import com.myshop.model.ItemVO;
import com.myshop.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 物品接口：支持换物（BARTER）、二手（SALE）、可卖可换（BOTH）。
 */
@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    /**
     * 物品列表，可按交易方式筛选。
     *
     * @param mode 交易方式，可选：BARTER / SALE / BOTH，不传返回全部
     * @return 物品列表
     */
    @GetMapping
    public Result<List<ItemVO>> list(@RequestParam(value = "mode", required = false) String mode) {
        return Result.success(itemService.listAll(mode));
    }

    /**
     * 物品详情。
     *
     * @param id 物品 ID
     * @return 物品详情，不存在时返回错误结果
     */
    @GetMapping("/{id}")
    public Result<ItemVO> detail(@PathVariable("id") Long id) {
        ItemVO item = itemService.getById(id);
        if (item == null) {
            return Result.error("物品不存在");
        }
        return Result.success(item);
    }
}
