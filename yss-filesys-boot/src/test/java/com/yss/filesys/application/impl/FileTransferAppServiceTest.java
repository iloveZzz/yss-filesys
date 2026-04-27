package com.yss.filesys.application.impl;

import com.yss.filesys.application.command.CheckUploadCommand;
import com.yss.filesys.application.command.InitTransferUploadCommand;
import com.yss.filesys.domain.gateway.FileRecordGateway;
import com.yss.filesys.domain.gateway.FileShareItemGateway;
import com.yss.filesys.domain.gateway.FileTransferTaskGateway;
import com.yss.filesys.domain.gateway.FileUserFavoriteGateway;
import com.yss.filesys.domain.model.BizException;
import com.yss.filesys.domain.model.FileRecord;
import com.yss.filesys.domain.model.FileTransferTask;
import com.yss.filesys.domain.model.TransferTaskStatus;
import com.yss.filesys.domain.model.TransferTaskType;
import com.yss.filesys.service.TransferSseService;
import com.yss.filesys.storage.plugin.boot.StorageServiceFacade;
import com.yss.filesys.storage.plugin.core.IStorageOperationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileTransferAppServiceTest {

    @Mock
    private FileTransferTaskGateway fileTransferTaskGateway;

    @Mock
    private FileRecordGateway fileRecordGateway;

    @Mock
    private FileShareItemGateway fileShareItemGateway;

    @Mock
    private FileUserFavoriteGateway fileUserFavoriteGateway;

    @Mock
    private StorageServiceFacade storageServiceFacade;

    @Mock
    private TransferSseService transferSseService;

    @Mock
    private IStorageOperationService storageOperationService;

    @InjectMocks
    private FileTransferAppService fileTransferAppService;

    @Test
    void should_reject_same_name_file_when_overwrite_disabled() {
        InitTransferUploadCommand command = new InitTransferUploadCommand();
        command.setUserId("user-001");
        command.setFileName("demo.txt");
        command.setFileSize(10L);
        command.setParentId("parent-001");
        command.setTotalChunks(1);
        command.setChunkSize(1024L);
        command.setMimeType("text/plain");
        command.setOverwriteExisting(Boolean.FALSE);

        FileRecord existed = FileRecord.builder()
                .fileId("old-001")
                .userId("user-001")
                .parentId("parent-001")
                .displayName("demo.txt")
                .originalName("demo.txt")
                .isDir(false)
                .build();
        when(fileRecordGateway.listByUserAndParentAndDeleted("user-001", "parent-001", false))
                .thenReturn(List.of(existed));

        BizException exception = assertThrows(BizException.class, () -> fileTransferAppService.initUpload(command));

        assertThat(exception.getMessage()).contains("同名文件");
        verify(fileTransferTaskGateway, never()).save(any());
    }

    @Test
    void should_replace_same_name_file_by_default_when_instant_upload_hits() {
        InitTransferUploadCommand command = new InitTransferUploadCommand();
        command.setUserId("user-001");
        command.setFileName("demo.txt");
        command.setFileSize(10L);
        command.setParentId("parent-001");
        command.setTotalChunks(1);
        command.setChunkSize(1024L);
        command.setMimeType("text/plain");

        FileTransferTask initializedTask = FileTransferTask.builder()
                .taskId("task-001")
                .userId("user-001")
                .parentId("parent-001")
                .fileName("demo.txt")
                .fileSize(10L)
                .fileMd5("md5-new")
                .suffix("txt")
                .mimeType("text/plain")
                .totalChunks(1)
                .chunkSize(1024L)
                .taskType(TransferTaskType.upload)
                .uploadedChunks(0)
                .uploadedSize(0L)
                .overwriteExisting(Boolean.TRUE)
                .status(TransferTaskStatus.initialized)
                .build();
        when(fileTransferTaskGateway.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(fileTransferTaskGateway.findByTaskId("task-001")).thenReturn(Optional.of(initializedTask));

        FileRecord source = FileRecord.builder()
                .fileId("source-001")
                .userId("user-001")
                .parentId("other-parent")
                .displayName("source.txt")
                .originalName("source.txt")
                .objectKey("object-key-source")
                .storageSettingId("storage-001")
                .isDir(false)
                .build();
        when(fileRecordGateway.findByUserAndMd5("user-001", "md5-new")).thenReturn(Optional.of(source));
        when(storageServiceFacade.getStorageService("storage-001")).thenReturn(storageOperationService);
        when(storageOperationService.isFileExist("object-key-source")).thenReturn(true);

        FileRecord oldSameName = FileRecord.builder()
                .fileId("old-001")
                .userId("user-001")
                .parentId("parent-001")
                .displayName("demo.txt")
                .originalName("demo.txt")
                .objectKey("object-key-old")
                .storageSettingId("storage-001")
                .isDir(false)
                .build();
        when(fileRecordGateway.listByUserAndParentAndDeleted("user-001", "parent-001", false))
                .thenReturn(List.of(oldSameName));
        when(fileRecordGateway.countByObjectKeyExcludingIds("object-key-old", List.of("old-001")))
                .thenReturn(0L);

        CheckUploadCommand checkUploadCommand = new CheckUploadCommand();
        checkUploadCommand.setTaskId("task-001");
        checkUploadCommand.setFileMd5("md5-new");

        var result = fileTransferAppService.checkUpload(checkUploadCommand);

        assertThat(result.isInstantUpload()).isTrue();
        verify(fileRecordGateway).deleteByIds(List.of("old-001"));
        verify(fileShareItemGateway).deleteByFileIds(List.of("old-001"));
        verify(fileUserFavoriteGateway).deleteByUserAndFileIds("user-001", List.of("old-001"));
        verify(storageOperationService).deleteFile("object-key-old");
        ArgumentCaptor<FileTransferTask> taskCaptor = ArgumentCaptor.forClass(FileTransferTask.class);
        verify(fileTransferTaskGateway).save(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getOverwriteExisting()).isTrue();
    }
}
