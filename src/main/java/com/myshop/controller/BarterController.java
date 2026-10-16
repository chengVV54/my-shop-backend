package com.myshop.controller;

import com.myshop.common.Result;
import com.myshop.model.BarterRequestVO;
import com.myshop.model.CreateBarterRequest;
import com.myshop.service.BarterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 换物申请接口，数据来自 MySQL 的 barter_requests 表。
 *
 * <p>{@code /api/barters/**} 在 {@code SecurityConfig} 中要求登录，
 * 因此 Controller 里不做鉴权判断，未登录的请求会在进入本类之前被拦截成 401。
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

    /**
     * 发起换物申请（1:1），需要登录。
     *
     * @param req 换物请求体：toItemId（想换的）、fromItemId（自己提供的）、message（选填）
     * @return 新建的换物申请
     */
    @PostMapping
    public Result<BarterRequestVO> create(@Valid @RequestBody CreateBarterRequest req) {
        return Result.success(barterService.create(req));
    }
}
