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
import com.yss.filesys.feignsdk.properties.YssFilesysFeignSdkProperties;
import com.yss.filesys.feignsdk.support.ByteArrayMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * yss-filesys 传输任务门面。
 * <p>
 * 统一封装初始化、校验、分片上传、合并和已上传分片查询等操作。
 * </p>
 */
@Service
public class YssFilesysTransferSdkService {

    private final YssFilesysTransferFeignClient feignClient;
    private final YssFilesysFeignSdkProperties properties;

    public YssFilesysTransferSdkService(YssFilesysTransferFeignClient feignClient,
                                        YssFilesysFeignSdkProperties properties) {
        this.feignClient = feignClient;
        this.properties = properties;
    }

    /**
     * 初始化上传任务。
     *
     * @param request 初始化请求
     * @return 传输任务信息
     */
    public YssFilesysTransferTaskDTO initUpload(YssFilesysInitUploadRequest request) {
        return requireData(feignClient.initUpload(request), "初始化上传任务失败");
    }

    /**
     * 执行秒传校验。
     *
     * @param request 校验请求
     * @return 校验结果
     */
    public YssFilesysCheckUploadResultDTO checkUpload(YssFilesysCheckUploadRequest request) {
        return requireData(feignClient.checkUpload(request), "上传校验失败");
    }

    /**
     * 上传分片。
     *
     * @param file 分片文件
     * @param taskId 传输任务ID
     * @param chunkIndex 分片序号
     * @param chunkMd5 分片MD5
     */
    public void uploadChunk(MultipartFile file, String taskId, Integer chunkIndex, String chunkMd5) {
        requireSuccess(feignClient.uploadChunk(file, taskId, chunkIndex, chunkMd5), "上传分片失败");
    }

    /**
     * 上传分片。
     *
     * @param content 分片内容
     * @param filename 分片文件名
     * @param taskId 传输任务ID
     * @param chunkIndex 分片序号
     * @param chunkMd5 分片MD5
     */
    public void uploadChunk(byte[] content, String filename, String taskId, Integer chunkIndex, String chunkMd5) {
        MultipartFile file = new ByteArrayMultipartFile("file", filename, content);
        uploadChunk(file, taskId, chunkIndex, chunkMd5);
    }

    /**
     * 合并所有分片。
     *
     * @param request 合并请求
     * @return 最终文件记录
     */
    public YssFilesysFileRecordDTO mergeChunks(YssFilesysMergeChunksRequest request) {
        return requireData(feignClient.mergeChunks(request), "合并分片失败");
    }

    /**
     * 查询已上传分片序号。
     *
     * @param taskId 传输任务ID
     * @return 已上传分片序号列表
     */
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
