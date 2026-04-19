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
import com.yss.filesys.feignsdk.exception.YssFilesysFeignSdkException;
import com.yss.filesys.feignsdk.support.ByteArrayMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * yss-filesys 文件上传门面。
 */
@Service
public class YssFilesysTransferSdkService {

    private final YssFilesysTransferFeignClient feignClient;

    public YssFilesysTransferSdkService(YssFilesysTransferFeignClient feignClient) {
        this.feignClient = feignClient;
    }

    public YssFilesysTransferTaskDTO initUpload(YssFilesysInitUploadRequest request) {
        return requireData(feignClient.initUpload(request), "初始化上传任务失败");
    }

    public YssFilesysCheckUploadResultDTO checkUpload(YssFilesysCheckUploadRequest request) {
        return requireData(feignClient.checkUpload(request), "上传校验失败");
    }

    public void uploadChunk(MultipartFile file, String taskId, Integer chunkIndex, String chunkMd5) {
        requireSuccess(feignClient.uploadChunk(file, taskId, chunkIndex, chunkMd5), "上传分片失败");
    }

    public void uploadChunk(byte[] content, String filename, String taskId, Integer chunkIndex, String chunkMd5) {
        MultipartFile file = new ByteArrayMultipartFile("file", filename, content);
        uploadChunk(file, taskId, chunkIndex, chunkMd5);
    }

    public YssFilesysFileRecordDTO mergeChunks(YssFilesysMergeChunksRequest request) {
        return requireData(feignClient.mergeChunks(request), "合并分片失败");
    }

    public List<Integer> getUploadedChunks(String taskId) {
        return requireData(feignClient.getUploadedChunks(taskId), "查询已上传分片失败");
    }

    private static <T> T requireData(SingleResult<T> response, String message) {
        requireSuccess(response, message);
        return response.getData();
    }

    private static <T> java.util.List<T> requireData(MultiResult<T> response, String message) {
        requireSuccess(response, message);
        return response.getData();
    }

    private static void requireSuccess(com.yss.cloud.dto.response.Result response, String message) {
        if (response == null) {
            throw new YssFilesysFeignSdkException(message + "，返回结果为空");
        }
        if (!response.isSuccess()) {
            throw new YssFilesysFeignSdkException(message + "，code=" + response.getCode() + "，message=" + response.getMessage());
        }
    }
}
