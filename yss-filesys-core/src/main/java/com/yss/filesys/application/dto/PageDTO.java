package com.yss.filesys.application.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class PageDTO<T> {
    long total;
    long pageNo;
    long pageSize;
    List<T> records;
}
