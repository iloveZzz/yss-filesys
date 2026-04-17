package com.yss.filesys.common;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ApiResponse<T> {
    boolean success;
    String message;
    T data;

    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder().success(true).message("OK").data(data).build();
    }

    public static ApiResponse<Void> ok() {
        return ApiResponse.<Void>builder().success(true).message("OK").build();
    }

    public static ApiResponse<Void> fail(String message) {
        return ApiResponse.<Void>builder().success(false).message(message).build();
    }
}
