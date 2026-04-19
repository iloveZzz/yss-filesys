package com.yss.filesys.controller;

import com.yss.filesys.application.dto.FileHomeDTO;
import com.yss.filesys.application.port.FileHomeUseCase;
import com.yss.filesys.common.AnonymousUserContext;
import com.yss.cloud.dto.response.SingleResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文件首页控制器
 * <p>
 * 提供文件首页统计信息接口
 * </p>
 */
@RestController
@RequestMapping("/files/home")
@Tag(name = "文件首页")
@RequiredArgsConstructor
public class FileHomeController {

    /**
     * 文件首页用例
     */
    private final FileHomeUseCase fileHomeUseCase;

    /**
     * 获取文件首页统计信息
     * @return 首页统计信息
     */
    @GetMapping
    @Operation(summary = "获取文件首页统计")
    public SingleResult<FileHomeDTO> getFileHomeStats(@RequestParam(required = false) Integer unit,
                                          @RequestParam(required = false) Integer dateType) {
        return SingleResult.of(fileHomeUseCase.getHome(AnonymousUserContext.userId(), unit, dateType));
    }
}
