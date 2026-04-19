package com.yss.filesys.application.port;

import com.yss.filesys.application.dto.FileDownloadDTO;
import com.yss.filesys.application.dto.DirectoryTreeDTO;
import com.yss.filesys.application.dto.FileRecordDTO;
import com.yss.filesys.application.dto.PageDTO;
import com.yss.filesys.application.query.FileSearchQuery;

import java.util.List;

public interface FileQueryUseCase {

    PageDTO<FileRecordDTO> search(FileSearchQuery query);

    FileRecordDTO getById(String fileId);

    List<DirectoryTreeDTO> listDirs(String userId, String parentId);

    List<FileRecordDTO> getDirectoryTreePath(String userId, String dirId);

    String getFileUrl(String fileId, String userId, Integer expireSeconds);

    FileDownloadDTO downloadFile(String fileId, String userId);

    /**
     * 批量下载文件
     * @param fileIds 文件ID列表
     * @param userId 用户ID
     * @return 文件下载DTO列表
     */
    List<FileDownloadDTO> downloadFiles(List<String> fileIds, String userId);
}
