package com.myshop.controller;

import com.myshop.common.Result;
import com.myshop.model.BarterRequestVO;
import com.myshop.service.BarterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 换物申请接口（当前为假数据）。
 */
@RestController
@RequestMapping("/api/barters")
@RequiredArgsConstructor
public class BarterController {

    private final BarterService barterService;

    /**
     * 换物申请列表。
     *
     * @return 换物申请列表
     */
    @GetMapping
    public Result<List<BarterRequestVO>> list() {
        return Result.success(barterService.listAll());
    }
}
