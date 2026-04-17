package com.yss.filesys.application.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * 分页DTO
 * <p>
 * 通用分页数据传输对象
 * </p>
 *
 * @param <T> 数据类型
 */
@Value
@Builder
public class PageDTO<T> {
    /**
     * 总记录数
     */
    long total;
    /**
     * 当前页码
     */
    long pageNo;
    /**
     * 每页大小
     */
    long pageSize;
    /**
     * 数据列表
     */
    List<T> records;
}
