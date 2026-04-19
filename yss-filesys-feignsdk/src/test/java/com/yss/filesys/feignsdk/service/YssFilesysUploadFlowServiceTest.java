package com.yss.filesys.feignsdk.service;

import com.yss.cloud.dto.response.MultiResult;
import com.yss.cloud.dto.response.SingleResult;
import com.yss.filesys.feignsdk.client.YssFilesysTransferFeignClient;
import com.yss.filesys.feignsdk.dto.YssFilesysCheckUploadRequest;
import com.yss.filesys.feignsdk.dto.YssFilesysCheckUploadResultDTO;
import com.yss.filesys.feignsdk.dto.YssFilesysFileRecordDTO;
import com.yss.filesys.feignsdk.dto.YssFilesysInitUploadRequest;
import com.yss.filesys.feignsdk.dto.YssFilesysMergeChunksRequest;
import com.yss.filesys.feignsdk.dto.YssFilesysTransferTaskDTO;
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

    @Autowired
    private YssFilesysUploadFlowService uploadFlowService;

    @Autowired
    private FakeTransferFeignClient fakeTransferFeignClient;

    @Test
    void should_run_full_upload_flow() throws IOException {
        byte[] content = new byte[]{1, 2, 3, 4, 5, 6, 7};
        var result = uploadFlowService.upload(content, "demo.txt", "text/plain", "parent-001", null, 100L * 1024 * 1024);

        assertThat(result.isInstantUpload()).isFalse();
        assertThat(result.getTaskId()).isEqualTo("task-001");
        assertThat(result.getTransferTask().getTaskId()).isEqualTo("task-001");
        assertThat(result.getCheckResult().getTaskId()).isEqualTo("task-001");
        assertThat(result.getFileRecord().getFileId()).isEqualTo("file-001");
        assertThat(fakeTransferFeignClient.initUploadCount.get()).isEqualTo(1);
        assertThat(fakeTransferFeignClient.checkUploadCount.get()).isEqualTo(1);
        assertThat(fakeTransferFeignClient.getUploadedChunksCount.get()).isEqualTo(1);
        assertThat(fakeTransferFeignClient.uploadChunkCount.get()).isEqualTo(2);
        assertThat(fakeTransferFeignClient.mergeChunksCount.get()).isEqualTo(1);
        assertThat(fakeTransferFeignClient.lastInitUploadRequest.getFileName()).isEqualTo("demo.txt");
        assertThat(fakeTransferFeignClient.lastCheckUploadRequest.getFileMd5()).isNotBlank();
        assertThat(fakeTransferFeignClient.lastMergeChunksRequest.getTaskId()).isEqualTo("task-001");
    }
}
