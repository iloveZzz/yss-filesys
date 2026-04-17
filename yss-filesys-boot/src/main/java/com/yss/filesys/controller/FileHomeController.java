package com.yss.filesys.controller;

import com.yss.filesys.application.dto.FileHomeDTO;
import com.yss.filesys.application.port.FileHomeUseCase;
import com.yss.filesys.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/files/home")
@Tag(name = "文件首页")
@RequiredArgsConstructor
public class FileHomeController {

    private final FileHomeUseCase fileHomeUseCase;

    @GetMapping
    @Operation(summary = "获取文件首页统计")
    public ApiResponse<FileHomeDTO> home(@RequestParam String userId) {
        return ApiResponse.ok(fileHomeUseCase.getHome(userId));
    }
}
