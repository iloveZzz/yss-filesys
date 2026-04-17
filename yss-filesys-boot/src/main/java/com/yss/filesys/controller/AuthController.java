package com.yss.filesys.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.yss.filesys.application.port.LoginLogUseCase;
import com.yss.filesys.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证")
@RequiredArgsConstructor
public class AuthController {

    private final LoginLogUseCase loginLogUseCase;

    @PostMapping("/login")
    @Operation(summary = "登录（开发版）")
    public ApiResponse<Map<String, Object>> login(@Validated @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        StpUtil.login(request.getUserId());
        loginLogUseCase.recordSuccess(
                request.getUserId(),
                request.getUserId(),
                resolveClientIp(servletRequest),
                null,
                resolveBrowser(servletRequest.getHeader("User-Agent")),
                resolveOs(servletRequest.getHeader("User-Agent"))
        );
        return ApiResponse.ok(Map.of(
                "userId", request.getUserId(),
                "tokenName", StpUtil.getTokenName(),
                "tokenValue", StpUtil.getTokenValue()
        ));
    }

    @PostMapping("/logout")
    @Operation(summary = "登出")
    public ApiResponse<Void> logout() {
        StpUtil.logout();
        return ApiResponse.ok();
    }

    @GetMapping("/me")
    @Operation(summary = "查询当前登录态")
    public ApiResponse<Map<String, Object>> me() {
        return ApiResponse.ok(Map.of(
                "isLogin", StpUtil.isLogin(),
                "loginId", StpUtil.isLogin() ? StpUtil.getLoginIdAsString() : null
        ));
    }

    @Data
    static class LoginRequest {
        @NotBlank
        private String userId;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private String resolveBrowser(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return null;
        }
        String ua = userAgent.toLowerCase();
        if (ua.contains("chrome")) {
            return "Chrome";
        }
        if (ua.contains("firefox")) {
            return "Firefox";
        }
        if (ua.contains("safari")) {
            return "Safari";
        }
        if (ua.contains("edge")) {
            return "Edge";
        }
        if (ua.contains("micromessenger")) {
            return "WeChat";
        }
        return "Unknown";
    }

    private String resolveOs(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Unknown";
        }
        String ua = userAgent.toLowerCase();
        if (ua.contains("windows")) {
            return "Windows";
        }
        if (ua.contains("mac os") || ua.contains("macintosh")) {
            return "MacOS";
        }
        if (ua.contains("android")) {
            return "Android";
        }
        if (ua.contains("iphone") || ua.contains("ios")) {
            return "iOS";
        }
        if (ua.contains("linux")) {
            return "Linux";
        }
        return "Unknown";
    }
}
