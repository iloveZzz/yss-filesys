package com.yss.filesys.feignsdk.client;

import com.yss.cloud.dto.response.MultiResult;
import com.yss.cloud.dto.response.SingleResult;
import com.yss.filesys.feignsdk.dto.YssFilesysCheckUploadRequest;
import com.yss.filesys.feignsdk.dto.YssFilesysCheckUploadResultDTO;
import com.yss.filesys.feignsdk.dto.YssFilesysFileRecordDTO;
import com.yss.filesys.feignsdk.dto.YssFilesysInitUploadRequest;
import com.yss.filesys.feignsdk.dto.YssFilesysMergeChunksRequest;
import com.yss.filesys.feignsdk.dto.YssFilesysTransferTaskDTO;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

/**
 * yss-filesys 文件传输 Feign 客户端。
 * <p>
 * 面向其他 Spring Cloud 服务提供上传任务初始化、秒传校验、分片上传、合并和分片查询能力。
 * </p>
 */
@FeignClient(
        name = "${yss.filesys.feign.name:yss-filesys}",
        path = "${yss.filesys.feign.path:/transfers}",
        contextId = "yssFilesysTransferFeignClient"
)
public interface YssFilesysTransferFeignClient {

    /**
     * 初始化上传任务。
     */
    @PostMapping("/upload/init")
    SingleResult<YssFilesysTransferTaskDTO> initUpload(@Valid @RequestBody YssFilesysInitUploadRequest request);

    /**
     * 上传前 MD5 校验。
     */
    @PostMapping("/upload/check")
    SingleResult<YssFilesysCheckUploadResultDTO> checkUpload(@Valid @RequestBody YssFilesysCheckUploadRequest request);

    /**
     * 上传分片。
     */
    @PostMapping(value = "/upload/chunk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    SingleResult<Void> uploadChunk(@RequestPart("file") MultipartFile file,
                                   @RequestParam("taskId") String taskId,
                                   @RequestParam("chunkIndex") Integer chunkIndex,
                                   @RequestParam(value = "chunkMd5", required = false) String chunkMd5);

    /**
     * 手动触发分片合并。
     */
    @PostMapping("/upload/merge")
    SingleResult<YssFilesysFileRecordDTO> mergeChunks(@Valid @RequestBody YssFilesysMergeChunksRequest request);

    /**
     * 查询已上传分片。
     */
    @GetMapping("/chunks/{taskId}")
    MultiResult<Integer> getUploadedChunks(@PathVariable String taskId);
}
