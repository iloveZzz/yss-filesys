package com.yss.filesys.application.port;

import com.yss.filesys.application.dto.FileTransferTaskDTO;
import com.yss.filesys.application.query.DownloadChunkQuery;

import java.util.List;

public interface FileTransferQueryUseCase {

    List<FileTransferTaskDTO> listByUserId(String userId);

    FileTransferTaskDTO getByTaskId(String taskId);

    byte[] downloadChunk(DownloadChunkQuery query);
}
