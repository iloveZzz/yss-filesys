package com.yss.filesys.application.query;

import lombok.Data;

import java.util.Collection;

@Data
public class FileSearchQuery {
    private String userId;
    private String parentId;
    private String keyword;
    private Boolean deleted;
    private Boolean favorite;
    private Collection<String> fileIds;
    private long pageNo = 1;
    private long pageSize = 20;
}
