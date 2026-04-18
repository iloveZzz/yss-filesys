package com.yss.filesys.common;

import lombok.Builder;
import lombok.Value;

import java.io.Serializable;
import java.util.List;

@Value
@Builder
public class MultiResult<T> implements Serializable {
    boolean success;
    String message;
    @Builder.Default
    List<T> data = List.of();

    public static <T> MultiResult<T> ok(List<T> data) {
        return MultiResult.<T>builder().success(true).message("OK").data(data == null ? List.of() : data).build();
    }

    public static <T> MultiResult<T> fail(String message) {
        return MultiResult.<T>builder().success(false).message(message).data(List.of()).build();
    }
}
