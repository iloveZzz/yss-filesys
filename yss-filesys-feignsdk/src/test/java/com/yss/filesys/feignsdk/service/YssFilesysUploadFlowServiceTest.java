package com.yss.filesys.feignsdk.service;

import com.yss.cloud.dto.response.MultiResult;
import com.yss.cloud.dto.response.SingleResult;
import com.yss.filesys.feignsdk.client.YssFilesysTransferFeignClient;
import com.yss.filesys.feignsdk.dto.YssFilesysCheckUploadRequest;
import com.yss.filesys.feignsdk.dto.YssFilesysCheckUploadResultDTO;
import com.yss.filesys.feignsdk.dto.YssFilesysCreateDirectoryRequest;
import com.yss.filesys.feignsdk.dto.YssFilesysDirectoryTreeDTO;
import com.yss.filesys.feignsdk.dto.YssFilesysFileRecordDTO;
import com.yss.filesys.feignsdk.dto.YssFilesysInitUploadRequest;
import com.yss.filesys.feignsdk.dto.YssFilesysMergeChunksRequest;
import com.yss.filesys.feignsdk.dto.YssFilesysTransferTaskDTO;
import com.yss.filesys.feignsdk.dto.YssFilesysUploadToDirectoryRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 整体上传流程测试。
 */
@SpringBootTest(classes = YssFilesysUploadFlowServiceTest.TestConfig.class)
class YssFilesysUploadFlowServiceTest {

    @Configuration
    @EnableConfigurationProperties(com.yss.filesys.feignsdk.properties.YssFilesysFeignSdkProperties.class)
    @Import({YssFilesysTransferSdkService.class, YssFilesysUploadFlowService.class})
    static class TestConfig {

        @Bean
        @Primary
        FakeTransferFeignClient fakeTransferFeignClient() {
            return new FakeTransferFeignClient();
        }

        @Bean
        @Primary
        FakeFileSdkService fakeFileFeignClient() {
            return new FakeFileSdkService();
        }
    }

    static class FakeTransferFeignClient implements YssFilesysTransferFeignClient {

        private final AtomicInteger initUploadCount = new AtomicInteger();
        private final AtomicInteger checkUploadCount = new AtomicInteger();
        private final AtomicInteger uploadChunkCount = new AtomicInteger();
        private final AtomicInteger mergeChunksCount = new AtomicInteger();
        private final AtomicInteger getUploadedChunksCount = new AtomicInteger();
        private YssFilesysInitUploadRequest lastInitUploadRequest;
        private YssFilesysCheckUploadRequest lastCheckUploadRequest;
        private YssFilesysMergeChunksRequest lastMergeChunksRequest;

        @Override
        public SingleResult<YssFilesysTransferTaskDTO> initUpload(YssFilesysInitUploadRequest request) {
            this.initUploadCount.incrementAndGet();
            this.lastInitUploadRequest = request;
            return SingleResult.of(YssFilesysTransferTaskDTO.builder()
                    .taskId("task-001")
                    .build());
        }

        @Override
        public SingleResult<YssFilesysCheckUploadResultDTO> checkUpload(YssFilesysCheckUploadRequest request) {
            this.checkUploadCount.incrementAndGet();
            this.lastCheckUploadRequest = request;
            return SingleResult.of(YssFilesysCheckUploadResultDTO.builder()
                    .instantUpload(false)
                    .taskId("task-001")
                    .status("uploading")
                    .message("ok")
                    .build());
        }

        @Override
        public SingleResult<Void> uploadChunk(org.springframework.web.multipart.MultipartFile file, String taskId, Integer chunkIndex, String chunkMd5) {
            this.uploadChunkCount.incrementAndGet();
            return SingleResult.buildSuccess();
        }

        @Override
        public SingleResult<YssFilesysFileRecordDTO> mergeChunks(YssFilesysMergeChunksRequest request) {
            this.mergeChunksCount.incrementAndGet();
            this.lastMergeChunksRequest = request;
            return SingleResult.of(YssFilesysFileRecordDTO.builder()
                    .fileId("file-001")
                    .displayName("demo.txt")
                    .build());
        }

        @Override
        public MultiResult<Integer> getUploadedChunks(String taskId) {
            this.getUploadedChunksCount.incrementAndGet();
            return MultiResult.of(Collections.emptyList());
        }
    }

    static class FakeFileSdkService extends YssFilesysFileSdkService {
        private final java.util.Map<String, java.util.List<YssFilesysDirectoryTreeDTO>> directories = new java.util.HashMap<>();
        private final AtomicInteger createDirectoryCount = new AtomicInteger();
        private YssFilesysCreateDirectoryRequest lastCreateDirectoryRequest;

        FakeFileSdkService() {
            super(null);
        }

        @Override
        public YssFilesysFileRecordDTO createDirectory(YssFilesysCreateDirectoryRequest request) {
            this.createDirectoryCount.incrementAndGet();
            this.lastCreateDirectoryRequest = request;
            String fileId = "dir-" + this.createDirectoryCount.get();
            YssFilesysFileRecordDTO dto = YssFilesysFileRecordDTO.builder()
                    .fileId(fileId)
                    .displayName(request.getFolderName())
                    .originalName(request.getFolderName())
                    .parentId(request.getParentId())
                    .storageSettingId(request.getStorageSettingId())
                    .isDir(true)
                    .build();
            directories.computeIfAbsent(request.getParentId(), ignored -> new java.util.ArrayList<>())
                    .add(YssFilesysDirectoryTreeDTO.builder()
                            .fileId(fileId)
                            .displayName(request.getFolderName())
                            .originalName(request.getFolderName())
                            .parentId(request.getParentId())
                            .storageSettingId(request.getStorageSettingId())
                            .isDir(true)
                            .build());
            return dto;
        }

        @Override
        public java.util.List<YssFilesysDirectoryTreeDTO> listDirs(String parentId) {
            return directories.getOrDefault(parentId, Collections.emptyList());
        }
    }

    @Autowired
    private YssFilesysUploadFlowService uploadFlowService;

    @Autowired
    private FakeTransferFeignClient fakeTransferFeignClient;

    @Autowired
    private FakeFileSdkService fakeFileFeignService;

    @Test
    void should_run_full_upload_flow() throws IOException {
        byte[] content = new byte[]{1, 2, 3, 4, 5, 6, 7};
        int initBefore = fakeTransferFeignClient.initUploadCount.get();
        int checkBefore = fakeTransferFeignClient.checkUploadCount.get();
        int uploadedBefore = fakeTransferFeignClient.getUploadedChunksCount.get();
        int chunkBefore = fakeTransferFeignClient.uploadChunkCount.get();
        int mergeBefore = fakeTransferFeignClient.mergeChunksCount.get();
        var result = uploadFlowService.upload(content, "demo.txt", "text/plain", "parent-001", null, 100L * 1024 * 1024);

        assertThat(result.isInstantUpload()).isFalse();
        assertThat(result.getTaskId()).isEqualTo("task-001");
        assertThat(result.getTransferTask().getTaskId()).isEqualTo("task-001");
        assertThat(result.getCheckResult().getTaskId()).isEqualTo("task-001");
        assertThat(result.getFileRecord().getFileId()).isEqualTo("file-001");
        assertThat(fakeTransferFeignClient.initUploadCount.get()).isEqualTo(initBefore + 1);
        assertThat(fakeTransferFeignClient.checkUploadCount.get()).isEqualTo(checkBefore + 1);
        assertThat(fakeTransferFeignClient.getUploadedChunksCount.get()).isEqualTo(uploadedBefore + 1);
        assertThat(fakeTransferFeignClient.uploadChunkCount.get()).isEqualTo(chunkBefore + 1);
        assertThat(fakeTransferFeignClient.mergeChunksCount.get()).isEqualTo(mergeBefore + 1);
        assertThat(fakeTransferFeignClient.lastInitUploadRequest.getFileName()).isEqualTo("demo.txt");
        assertThat(fakeTransferFeignClient.lastCheckUploadRequest.getFileMd5()).isNotBlank();
        assertThat(fakeTransferFeignClient.lastMergeChunksRequest.getTaskId()).isEqualTo("task-001");
    }

    @Test
    void should_create_missing_directories_when_uploading_to_directory_path() throws IOException {
        byte[] content = new byte[]{1, 2, 3, 4};
        int createBefore = fakeFileFeignService.createDirectoryCount.get();
        YssFilesysUploadToDirectoryRequest request = new YssFilesysUploadToDirectoryRequest();
        request.setDirectoryPath("a/b/c");
        request.setStorageSettingId("storage-001");
        request.setChunkSize(1024L);
        request.setOverwriteExisting(Boolean.FALSE);
        var result = uploadFlowService.uploadToDirectory(content, "demo.txt", "text/plain", request);

        assertThat(result.getTaskId()).isEqualTo("task-001");
        assertThat(fakeFileFeignService.createDirectoryCount.get()).isEqualTo(createBefore + 3);
        assertThat(fakeFileFeignService.lastCreateDirectoryRequest.getFolderName()).isEqualTo("c");
        assertThat(fakeTransferFeignClient.lastInitUploadRequest.getParentId()).isEqualTo("dir-" + fakeFileFeignService.createDirectoryCount.get());
        assertThat(fakeTransferFeignClient.lastInitUploadRequest.getStorageSettingId()).isEqualTo("storage-001");
        assertThat(fakeTransferFeignClient.lastInitUploadRequest.getOverwriteExisting()).isFalse();
    }

    @Test
    void should_upload_byte_content_to_directory_path_via_new_overload() throws IOException {
        byte[] content = new byte[]{8, 9, 10};
        int createBefore = fakeFileFeignService.createDirectoryCount.get();
        var result = uploadFlowService.upload(
                content,
                "demo.txt",
                "text/plain",
                null,
                "docs/project",
                "storage-002",
                1024L
        );

        assertThat(result.getTaskId()).isEqualTo("task-001");
        assertThat(fakeFileFeignService.createDirectoryCount.get()).isEqualTo(createBefore + 2);
        assertThat(fakeFileFeignService.lastCreateDirectoryRequest.getFolderName()).isEqualTo("project");
        assertThat(fakeTransferFeignClient.lastInitUploadRequest.getParentId()).isEqualTo("dir-" + fakeFileFeignService.createDirectoryCount.get());
        assertThat(fakeTransferFeignClient.lastInitUploadRequest.getOverwriteExisting()).isTrue();
    }
}
