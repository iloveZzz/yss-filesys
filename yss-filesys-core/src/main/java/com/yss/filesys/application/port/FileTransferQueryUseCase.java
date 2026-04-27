package com.yss.filesys.application.port;

import com.yss.filesys.application.dto.FileTransferTaskDTO;
import com.yss.filesys.application.dto.FileTransferStatsDTO;
import com.yss.filesys.application.dto.PageDTO;
import com.yss.filesys.application.query.DownloadChunkQuery;

import java.util.List;

public interface FileTransferQueryUseCase {

    List<FileTransferTaskDTO> listByUserId(String userId, Integer statusType);

    PageDTO<FileTransferTaskDTO> pageByUserId(String userId, Integer statusType, String keyword, long pageIndex, long pageSize);

    FileTransferStatsDTO getStats(String userId);

    FileTransferTaskDTO getByTaskId(String taskId);

    byte[] downloadChunk(DownloadChunkQuery query);

    List<Integer> getUploadedChunks(String taskId);

    List<Integer> getDownloadedChunks(String taskId);
}
