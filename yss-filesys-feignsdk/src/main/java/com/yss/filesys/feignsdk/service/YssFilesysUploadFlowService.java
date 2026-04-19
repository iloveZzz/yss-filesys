package com.yss.filesys.feignsdk.service;

import com.yss.cloud.dto.response.MultiResult;
import com.yss.filesys.feignsdk.dto.YssFilesysCheckUploadRequest;
import com.yss.filesys.feignsdk.dto.YssFilesysCheckUploadResultDTO;
import com.yss.filesys.feignsdk.dto.YssFilesysFileRecordDTO;
import com.yss.filesys.feignsdk.dto.YssFilesysInitUploadRequest;
import com.yss.filesys.feignsdk.dto.YssFilesysMergeChunksRequest;
import com.yss.filesys.feignsdk.dto.YssFilesysTransferTaskDTO;
import com.yss.filesys.feignsdk.dto.YssFilesysUploadFlowResult;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/**
 * 整体上传流程服务。
 * <p>
 * 串联初始化、秒传校验、分片上传和合并，供外部服务直接调用。
 * </p>
 */
@Service
public class YssFilesysUploadFlowService {

    private static final long DEFAULT_CHUNK_SIZE = 5L * 1024 * 1024;

    private final YssFilesysTransferSdkService transferSdkService;

    public YssFilesysUploadFlowService(YssFilesysTransferSdkService transferSdkService) {
        this.transferSdkService = transferSdkService;
    }

    public YssFilesysUploadFlowResult upload(MultipartFile file,
                                             String parentId,
                                             String storageSettingId) throws IOException {
        return upload(file, parentId, storageSettingId, DEFAULT_CHUNK_SIZE);
    }

    public YssFilesysUploadFlowResult upload(MultipartFile file,
                                             String parentId,
                                             String storageSettingId,
                                             long chunkSize) throws IOException {
        String fileName = file.getOriginalFilename() == null ? file.getName() : file.getOriginalFilename();
        String mimeType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
        return upload(file.getBytes(), fileName, mimeType, parentId, storageSettingId, chunkSize);
    }

    public YssFilesysUploadFlowResult upload(byte[] content,
                                             String fileName,
                                             String mimeType,
                                             String parentId,
                                             String storageSettingId,
                                             long chunkSize) {
        byte[] source = content == null ? new byte[0] : content;
        long safeChunkSize = chunkSize <= 0 ? DEFAULT_CHUNK_SIZE : chunkSize;
        int totalChunks = Math.max(1, (int) Math.ceil((double) source.length / (double) safeChunkSize));
        String fileMd5 = md5Hex(source);

        YssFilesysInitUploadRequest initRequest = new YssFilesysInitUploadRequest();
        initRequest.setFileName(fileName);
        initRequest.setFileSize((long) source.length);
        initRequest.setParentId(parentId);
        initRequest.setTotalChunks(totalChunks);
        initRequest.setChunkSize(safeChunkSize);
        initRequest.setMimeType(mimeType);
        initRequest.setStorageSettingId(storageSettingId);

        YssFilesysTransferTaskDTO transferTask = transferSdkService.initUpload(initRequest);
        YssFilesysCheckUploadRequest checkRequest = new YssFilesysCheckUploadRequest();
        checkRequest.setTaskId(transferTask.getTaskId());
        checkRequest.setFileMd5(fileMd5);
        YssFilesysCheckUploadResultDTO checkResult = transferSdkService.checkUpload(checkRequest);

        if (!checkResult.isInstantUpload()) {
            List<Integer> uploadedChunks = transferSdkService.getUploadedChunks(transferTask.getTaskId());
            for (int chunkIndex = 0; chunkIndex < totalChunks; chunkIndex++) {
                if (uploadedChunks.contains(chunkIndex)) {
                    continue;
                }
                int start = (int) Math.min((long) chunkIndex * safeChunkSize, source.length);
                int end = (int) Math.min(start + safeChunkSize, source.length);
                byte[] chunk = slice(source, start, end);
                String chunkMd5 = md5Hex(chunk);
                transferSdkService.uploadChunk(chunk, buildChunkFileName(fileName, chunkIndex), transferTask.getTaskId(), chunkIndex, chunkMd5);
            }
        }

        YssFilesysMergeChunksRequest mergeRequest = new YssFilesysMergeChunksRequest();
        mergeRequest.setTaskId(transferTask.getTaskId());
        YssFilesysFileRecordDTO fileRecord = transferSdkService.mergeChunks(mergeRequest);

        return YssFilesysUploadFlowResult.builder()
                .taskId(transferTask.getTaskId())
                .instantUpload(checkResult.isInstantUpload())
                .transferTask(transferTask)
                .checkResult(checkResult)
                .fileRecord(fileRecord)
                .build();
    }

    private static String buildChunkFileName(String fileName, int chunkIndex) {
        return fileName + ".part-" + chunkIndex;
    }

    private static byte[] slice(byte[] source, int start, int end) {
        int safeStart = Math.max(0, Math.min(start, source.length));
        int safeEnd = Math.max(safeStart, Math.min(end, source.length));
        byte[] chunk = new byte[safeEnd - safeStart];
        System.arraycopy(source, safeStart, chunk, 0, chunk.length);
        return chunk;
    }

    private static String md5Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] result = digest.digest(bytes == null ? new byte[0] : bytes);
            return HexFormat.of().formatHex(result);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 算法不可用", e);
        }
    }
}
