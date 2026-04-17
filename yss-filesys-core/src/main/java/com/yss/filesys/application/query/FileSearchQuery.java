package com.yss.filesys.application.query;

import lombok.Data;

import java.util.Collection;

@Data
public class FileSearchQuery {
    private String userId;
    private String parentId;
    private String keyword;
    private String fileType;
    private Boolean deleted;
    private Boolean favorite;
    private Boolean isDir;
    private Boolean isRecents;
    private Collection<String> fileIds;
    private long pageNo = 1;
    private long pageSize = 20;
}
