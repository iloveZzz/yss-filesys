package com.yss.filesys.application.query;

import lombok.Data;

@Data
public class LoginLogPageQuery {
    private String userId;
    private String keyword;
    private Integer status;
    private long pageNo = 1;
    private long pageSize = 20;
}
