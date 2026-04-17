package com.yss.filesys.application.port;

import com.yss.filesys.application.command.CheckUploadCommand;
import com.yss.filesys.application.command.InitDownloadCommand;
import com.yss.filesys.application.command.InitTransferUploadCommand;
import com.yss.filesys.application.command.MergeChunksCommand;
import com.yss.filesys.application.command.UploadChunkCommand;
import com.yss.filesys.application.dto.CheckUploadResultDTO;
import com.yss.filesys.application.dto.FileTransferTaskDTO;
import com.yss.filesys.application.dto.InitDownloadResultDTO;
import com.yss.filesys.domain.model.FileRecord;

public interface FileTransferCommandUseCase {

    FileTransferTaskDTO initUpload(InitTransferUploadCommand command);

    CheckUploadResultDTO checkUpload(CheckUploadCommand command);

    void uploadChunk(UploadChunkCommand command, byte[] bytes);

    FileRecord mergeChunks(MergeChunksCommand command);

    InitDownloadResultDTO initDownload(InitDownloadCommand command);

    void pause(String taskId);

    void resume(String taskId);

    void cancel(String taskId);

    void clearFinished(String userId);
}
