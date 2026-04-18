package com.yss.filesys.controller;

import com.yss.filesys.application.command.SubscriptionPlanAddCommand;
import com.yss.filesys.application.command.SubscriptionPlanEditCommand;
import com.yss.filesys.application.dto.PageDTO;
import com.yss.filesys.application.dto.SubscriptionPlanDTO;
import com.yss.filesys.application.impl.SubscriptionPlanAppService;
import com.yss.filesys.application.query.SubscriptionPlanPageQuery;
import com.yss.cloud.dto.response.PageResult;
import com.yss.cloud.dto.response.SingleResult;
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

/**
 * 套餐管理控制器
 * <p>
 * 提供套餐的增删改查接口
 * </p>
 */
@RestController
@RequestMapping("/plans")
@Tag(name = "套餐管理")
@Validated
public class SubscriptionPlanController {

    /**
     * 套餐应用服务
     */
    private final SubscriptionPlanAppService subscriptionPlanAppService;

    public SubscriptionPlanController(SubscriptionPlanAppService subscriptionPlanAppService) {
        this.subscriptionPlanAppService = subscriptionPlanAppService;
    }

    /**
     * 分页获取套餐列表
     *
     * @param query 查询条件
     * @return 套餐分页数据
     */
    @GetMapping("/pages")
    @Operation(summary = "分页获取套餐列表")
    public PageResult<SubscriptionPlanDTO> getPages(SubscriptionPlanPageQuery query) {
        com.yss.filesys.application.dto.PageDTO<SubscriptionPlanDTO> result = subscriptionPlanAppService.page(query);
        return PageResult.of(result.getRecords(), result.getTotal(), result.getPageSize(), result.getPageNo());
    }

    /**
     * 获取套餐详细信息
     *
     * @param id 套餐ID
     * @return 套餐详情
     */
    @GetMapping("/info/{id}")
    @Operation(summary = "获取套餐详细信息")
    public SingleResult<SubscriptionPlanDTO> getDetail(@PathVariable Long id) {
        return SingleResult.of(subscriptionPlanAppService.detail(id));
    }

    /**
     * 添加套餐
     *
     * @param command 添加命令
     * @return 操作结果
     */
    @PostMapping
    @Operation(summary = "添加套餐")
    public SingleResult<Void> add(@Valid @RequestBody SubscriptionPlanAddCommand command) {
        subscriptionPlanAppService.add(command);
        return SingleResult.buildSuccess();
    }

    /**
     * 编辑套餐
     *
     * @param command 编辑命令
     * @return 操作结果
     */
    @PutMapping
    @Operation(summary = "编辑套餐")
    public SingleResult<Void> edit(@Valid @RequestBody SubscriptionPlanEditCommand command) {
        subscriptionPlanAppService.edit(command);
        return SingleResult.buildSuccess();
    }

    /**
     * 根据ID删除套餐
     *
     * @param id 套餐ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "根据ID删除套餐")
    public SingleResult<Void> delete(@PathVariable Long id) {
        subscriptionPlanAppService.delete(id);
        return SingleResult.buildSuccess();
    }
}
