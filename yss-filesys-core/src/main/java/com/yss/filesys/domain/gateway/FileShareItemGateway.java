package com.yss.filesys.domain.gateway;

import com.yss.filesys.domain.model.FileShareItem;

import java.util.Collection;
import java.util.List;

public interface FileShareItemGateway {

    void saveBatch(List<FileShareItem> items);

    List<FileShareItem> listByShareId(String shareId);

    void deleteByShareIds(List<String> shareIds);

    void deleteByFileIds(Collection<String> fileIds);
}
