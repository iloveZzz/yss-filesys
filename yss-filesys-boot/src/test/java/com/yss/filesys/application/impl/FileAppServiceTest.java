package com.yss.filesys.application.impl;

import com.yss.filesys.application.command.CreateDirectoryCommand;
import com.yss.filesys.domain.gateway.FileRecordGateway;
import com.yss.filesys.domain.gateway.FileShareItemGateway;
import com.yss.filesys.domain.gateway.FileUserFavoriteGateway;
import com.yss.filesys.domain.model.BizException;
import com.yss.filesys.domain.model.FileRecord;
import com.yss.filesys.storage.plugin.boot.StorageServiceFacade;
import com.yss.filesys.storage.plugin.core.IStorageOperationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileAppServiceTest {

    @Mock
    private FileRecordGateway fileRecordGateway;

    @Mock
    private FileShareItemGateway fileShareItemGateway;

    @Mock
    private FileUserFavoriteGateway fileUserFavoriteGateway;

    @Mock
    private StorageServiceFacade storageServiceFacade;

    @Mock
    private IStorageOperationService storageOperationService;

    @InjectMocks
    private FileAppService fileAppService;

    @Test
    void should_reuse_existing_directory_when_same_name_directory_already_exists() {
        CreateDirectoryCommand command = new CreateDirectoryCommand();
        command.setUserId("user-001");
        command.setParentId("parent-001");
        command.setFolderName("20260427");
        command.setStorageSettingId("storage-001");

        FileRecord existedDirectory = FileRecord.builder()
                .fileId("dir-001")
                .userId("user-001")
                .parentId("parent-001")
                .displayName("20260427")
                .originalName("20260427")
                .isDir(true)
                .build();
        when(fileRecordGateway.listByUserAndParentAndDeleted("user-001", "parent-001", false))
                .thenReturn(List.of(existedDirectory));

        var result = fileAppService.createDirectory(command);

        assertThat(result.getFileId()).isEqualTo("dir-001");
        assertThat(result.getDisplayName()).isEqualTo("20260427");
        verify(fileRecordGateway, never()).save(any());
    }

    @Test
    void should_replace_when_same_name_file_exists_in_target_directory() {
        CreateDirectoryCommand command = new CreateDirectoryCommand();
        command.setUserId("user-001");
        command.setParentId("parent-001");
        command.setFolderName("20260427");
        command.setStorageSettingId("storage-001");

        FileRecord existedFile = FileRecord.builder()
                .fileId("file-001")
                .userId("user-001")
                .parentId("parent-001")
                .displayName("20260427")
                .originalName("20260427")
                .objectKey("object-key-old")
                .storageSettingId("storage-001")
                .isDir(false)
                .build();
        when(fileRecordGateway.listByUserAndParentAndDeleted("user-001", "parent-001", false))
                .thenReturn(List.of(existedFile));
        when(fileRecordGateway.countByObjectKeyExcludingIds("object-key-old", List.of("file-001")))
                .thenReturn(0L);
        when(storageServiceFacade.getStorageService("storage-001")).thenReturn(storageOperationService);
        when(fileRecordGateway.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(storageOperationService).deleteFile("object-key-old");

        var result = fileAppService.createDirectory(command);

        assertThat(result.getDisplayName()).isEqualTo("20260427");
        verify(fileRecordGateway).deleteByIds(List.of("file-001"));
        verify(storageOperationService).deleteFile("object-key-old");
        verify(fileRecordGateway).save(any());
    }

    @Test
    void should_merge_duplicate_directories_when_same_name_directories_exist() {
        CreateDirectoryCommand command = new CreateDirectoryCommand();
        command.setUserId("user-001");
        command.setParentId("parent-001");
        command.setFolderName("20260427");
        command.setStorageSettingId("storage-001");

        FileRecord canonicalDirectory = FileRecord.builder()
                .fileId("dir-canonical")
                .userId("user-001")
                .parentId("parent-001")
                .displayName("20260427")
                .originalName("20260427")
                .isDir(true)
                .build();
        FileRecord duplicateDirectory = FileRecord.builder()
                .fileId("dir-duplicate")
                .userId("user-001")
                .parentId("parent-001")
                .displayName("20260427")
                .originalName("20260427")
                .isDir(true)
                .build();
        FileRecord childFile = FileRecord.builder()
                .fileId("child-file")
                .userId("user-001")
                .parentId("dir-duplicate")
                .displayName("child.txt")
                .originalName("child.txt")
                .objectKey("child-object")
                .storageSettingId("storage-001")
                .isDir(false)
                .build();

        when(fileRecordGateway.listByUserAndParentAndDeleted(eq("user-001"), eq("parent-001"), eq(false)))
                .thenReturn(List.of(canonicalDirectory, duplicateDirectory));
        when(fileRecordGateway.listByUserAndParentAndDeleted(eq("user-001"), eq("dir-duplicate"), eq(false)))
                .thenReturn(List.of(childFile));
        when(fileRecordGateway.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = fileAppService.createDirectory(command);

        assertThat(result.getFileId()).isEqualTo("dir-canonical");
        verify(fileRecordGateway).save(argThat(record ->
                "child-file".equals(record.getFileId())
                        && "dir-canonical".equals(record.getParentId())
                        && "child.txt".equals(record.getDisplayName())
        ));
        verify(fileRecordGateway).deleteByIds(List.of("dir-duplicate"));
    }
}
