package com.yss.filesys.controller;

import com.yss.filesys.application.command.SubscriptionPlanAddCommand;
import com.yss.filesys.application.command.SubscriptionPlanEditCommand;
import com.yss.filesys.application.dto.PageDTO;
import com.yss.filesys.application.dto.SubscriptionPlanDTO;
import com.yss.filesys.application.impl.SubscriptionPlanAppService;
import com.yss.filesys.application.query.SubscriptionPlanPageQuery;
import com.yss.filesys.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/api/plans")
@Tag(name = "套餐管理")
@Validated
public class SubscriptionPlanController {

    private final SubscriptionPlanAppService subscriptionPlanAppService;

    public SubscriptionPlanController(SubscriptionPlanAppService subscriptionPlanAppService) {
        this.subscriptionPlanAppService = subscriptionPlanAppService;
    }

    @GetMapping("/pages")
    @Operation(summary = "分页获取套餐列表")
    public ApiResponse<PageDTO<SubscriptionPlanDTO>> getPages(SubscriptionPlanPageQuery query) {
        return ApiResponse.ok(subscriptionPlanAppService.page(query));
    }

    @GetMapping("/info/{id}")
    @Operation(summary = "获取套餐详细信息")
    public ApiResponse<SubscriptionPlanDTO> getDetail(@PathVariable Long id) {
        return ApiResponse.ok(subscriptionPlanAppService.detail(id));
    }

    @PostMapping
    @Operation(summary = "添加套餐")
    public ApiResponse<Void> add(@Valid @RequestBody SubscriptionPlanAddCommand command) {
        subscriptionPlanAppService.add(command);
        return ApiResponse.ok();
    }

    @PutMapping
    @Operation(summary = "编辑套餐")
    public ApiResponse<Void> edit(@Valid @RequestBody SubscriptionPlanEditCommand command) {
        subscriptionPlanAppService.edit(command);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "根据ID删除套餐")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        subscriptionPlanAppService.delete(id);
        return ApiResponse.ok();
    }
}
