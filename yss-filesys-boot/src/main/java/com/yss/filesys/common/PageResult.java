package com.yss.filesys.common;

import com.yss.filesys.application.dto.PageDTO;
import lombok.Builder;
import lombok.Value;

import java.io.Serializable;
import java.util.List;

@Value
@Builder
public class PageResult<T> implements Serializable {
    boolean success;
    String message;
    long total;
    long pageNo;
    long pageSize;
    @Builder.Default
    List<T> records = List.of();

    public static <T> PageResult<T> ok(PageDTO<T> page) {
        if (page == null) {
            return PageResult.<T>builder().success(true).message("OK").total(0L).pageNo(1L).pageSize(20L).records(List.of()).build();
        }
        return PageResult.<T>builder()
                .success(true)
                .message("OK")
                .total(page.getTotal())
                .pageNo(page.getPageNo())
                .pageSize(page.getPageSize())
                .records(page.getRecords() == null ? List.of() : page.getRecords())
                .build();
    }

    public static <T> PageResult<T> fail(String message) {
        return PageResult.<T>builder().success(false).message(message).total(0L).pageNo(1L).pageSize(20L).records(List.of()).build();
    }
}
