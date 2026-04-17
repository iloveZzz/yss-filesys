package com.yss.filesys.common;

import lombok.Builder;
import lombok.Value;

/**
 * 统一API响应对象
 * <p>
 * 用于封装所有API接口的返回结果
 * </p>
 *
 * @param <T> 数据类型
 */
@Value
@Builder
public class ApiResponse<T> {
    /**
     * 是否成功
     */
    boolean success;
    /**
     * 提示信息
     */
    String message;
    /**
     * 响应数据
     */
    T data;

    /**
     * 成功响应（带数据）
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 成功响应
     */
    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder().success(true).message("OK").data(data).build();
    }

    /**
     * 成功响应（无数据）
     *
     * @return 成功响应
     */
    public static ApiResponse<Void> ok() {
        return ApiResponse.<Void>builder().success(true).message("OK").build();
    }

    /**
     * 失败响应
     *
     * @param message 错误信息
     * @return 失败响应
     */
    public static ApiResponse<Void> fail(String message) {
        return ApiResponse.<Void>builder().success(false).message(message).build();
    }
}
