package com.yss.filesys.controller;

import com.yss.filesys.application.dto.LoginLogDTO;
import com.yss.filesys.application.dto.PageDTO;
import com.yss.filesys.application.port.LoginLogUseCase;
import com.yss.filesys.application.query.LoginLogPageQuery;
import com.yss.filesys.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/logs")
@Tag(name = "登录日志")
@RequiredArgsConstructor
public class LoginLogController {

    private final LoginLogUseCase loginLogUseCase;

    @GetMapping("/login/pages")
    @Operation(summary = "分页查询登录日志")
    public ApiResponse<PageDTO<LoginLogDTO>> pages(LoginLogPageQuery query) {
        return ApiResponse.ok(loginLogUseCase.page(query));
    }
}
