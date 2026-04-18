package com.yss.filesys.common;

import lombok.Builder;
import lombok.Value;

import java.io.Serializable;

@Value
@Builder
public class SingleResult<T> implements Serializable {
    boolean success;
    String message;
    T data;

    public static <T> SingleResult<T> ok(T data) {
        return SingleResult.<T>builder().success(true).message("OK").data(data).build();
    }

    public static SingleResult<Void> ok() {
        return SingleResult.<Void>builder().success(true).message("OK").build();
    }

    public static SingleResult<Void> fail(String message) {
        return SingleResult.<Void>builder().success(false).message(message).build();
    }
}
