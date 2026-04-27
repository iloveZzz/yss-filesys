package com.yss.filesys.feignsdk.service;

import com.yss.filesys.feignsdk.dto.YssFilesysCheckUploadRequest;
import com.yss.filesys.feignsdk.dto.YssFilesysCheckUploadResultDTO;
import com.yss.filesys.feignsdk.dto.YssFilesysCreateDirectoryRequest;
import com.yss.filesys.feignsdk.dto.YssFilesysDirectoryTreeDTO;
import com.yss.filesys.feignsdk.dto.YssFilesysFileRecordDTO;
import com.yss.filesys.feignsdk.dto.YssFilesysInitUploadRequest;
import com.yss.filesys.feignsdk.dto.YssFilesysMergeChunksRequest;
import com.yss.filesys.feignsdk.dto.YssFilesysTransferTaskDTO;
import com.yss.filesys.feignsdk.dto.YssFilesysUploadToDirectoryRequest;
import com.yss.filesys.feignsdk.dto.YssFilesysUploadFlowResult;
import com.yss.filesys.feignsdk.properties.YssFilesysFeignSdkProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
    private final YssFilesysFileSdkService fileSdkService;
    private final YssFilesysFeignSdkProperties properties;

    public YssFilesysUploadFlowService(YssFilesysTransferSdkService transferSdkService,
                                       YssFilesysFileSdkService fileSdkService,
                                       YssFilesysFeignSdkProperties properties) {
        this.transferSdkService = transferSdkService;
        this.fileSdkService = fileSdkService;
        this.properties = properties;
    }

    /**
     * 直接上传到指定父目录。
     *
     * @param file 文件
     * @param parentId 父目录ID
     * @param storageSettingId 存储配置ID
     * @return 上传流程结果
     * @throws IOException 读取文件内容失败时抛出
     */
    public YssFilesysUploadFlowResult upload(MultipartFile file,
                                             String parentId,
                                             String storageSettingId) throws IOException {
        return upload(file, parentId, storageSettingId, properties.getDefaultChunkSize());
    }

    /**
     * 按目录路径上传文件，目录不存在时自动创建。
     */
    public YssFilesysUploadFlowResult uploadToDirectory(MultipartFile file,
                                                        String directoryPath,
                                                        String storageSettingId) throws IOException {
        return uploadToDirectory(file, directoryPath, storageSettingId, properties.getDefaultChunkSize());
    }

    /**
     * 按目录路径上传文件，目录不存在时自动创建。
     */
    public YssFilesysUploadFlowResult uploadToDirectory(MultipartFile file,
                                                        YssFilesysUploadToDirectoryRequest request) throws IOException {
        long chunkSize = resolveChunkSize(request == null ? null : request.getChunkSize());
        String directoryPath = request == null ? null : request.getDirectoryPath();
        String storageSettingId = request == null ? null : request.getStorageSettingId();
        boolean overwriteExisting = resolveOverwriteExisting(request == null ? null : request.getOverwriteExisting());
        return uploadToDirectory(file, directoryPath, storageSettingId, chunkSize, overwriteExisting);
    }

    /**
     * 按目录路径上传文件，目录不存在时自动创建。
     */
    public YssFilesysUploadFlowResult uploadToDirectory(MultipartFile file,
                                                        String directoryPath,
                                                        String storageSettingId,
                                                        long chunkSize) throws IOException {
        return uploadToDirectory(file, directoryPath, storageSettingId, chunkSize, true);
    }

    /**
     * 按目录路径上传文件，目录不存在时自动创建。
     */
    public YssFilesysUploadFlowResult uploadToDirectory(MultipartFile file,
                                                        String directoryPath,
                                                        String storageSettingId,
                                                        long chunkSize,
                                                        boolean overwriteExisting) throws IOException {
        String fileName = file.getOriginalFilename() == null ? file.getName() : file.getOriginalFilename();
        String mimeType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
        return uploadToDirectory(file.getBytes(), fileName, mimeType, directoryPath, storageSettingId, chunkSize, overwriteExisting);
    }

    /**
     * 直接上传到指定父目录。
     *
     * @param file 文件
     * @param parentId 父目录ID
     * @param storageSettingId 存储配置ID
     * @param chunkSize 分片大小
     * @return 上传流程结果
     * @throws IOException 读取文件内容失败时抛出
     */
    public YssFilesysUploadFlowResult upload(MultipartFile file,
                                             String parentId,
                                             String storageSettingId,
                                             long chunkSize) throws IOException {
        String fileName = file.getOriginalFilename() == null ? file.getName() : file.getOriginalFilename();
        String mimeType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
        return upload(file.getBytes(), fileName, mimeType, parentId, storageSettingId, chunkSize);
    }

    /**
     * 直接上传本地文件到指定父目录。
     *
     * @param filePath 文件路径
     * @param parentId 父目录ID
     * @param storageSettingId 存储配置ID
     * @return 上传流程结果
     * @throws IOException 读取文件内容失败时抛出
     */
    public YssFilesysUploadFlowResult upload(Path filePath,
                                             String parentId,
                                             String storageSettingId) throws IOException {
        return upload(filePath, parentId, storageSettingId, properties.getDefaultChunkSize());
    }

    /**
     * 直接上传本地文件到指定父目录。
     *
     * @param filePath 文件路径
     * @param parentId 父目录ID
     * @param storageSettingId 存储配置ID
     * @param chunkSize 分片大小
     * @return 上传流程结果
     * @throws IOException 读取文件内容失败时抛出
     */
    public YssFilesysUploadFlowResult upload(Path filePath,
                                             String parentId,
                                             String storageSettingId,
                                             long chunkSize) throws IOException {
        if (filePath == null) {
            throw new IllegalArgumentException("文件路径不能为空");
        }
        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException("文件不存在: " + filePath);
        }
        byte[] content = Files.readAllBytes(filePath);
        String fileName = filePath.getFileName() == null ? filePath.toString() : filePath.getFileName().toString();
        String mimeType = Files.probeContentType(filePath);
        if (mimeType == null || mimeType.isBlank()) {
            mimeType = "application/octet-stream";
        }
        return upload(content, fileName, mimeType, parentId, storageSettingId, chunkSize);
    }

    /**
     * 按目录路径上传本地文件，目录不存在时自动创建。
     */
    public YssFilesysUploadFlowResult uploadToDirectory(Path filePath,
                                                        String directoryPath,
                                                        String storageSettingId) throws IOException {
        return uploadToDirectory(filePath, directoryPath, storageSettingId, properties.getDefaultChunkSize());
    }

    /**
     * 按目录路径上传本地文件，目录不存在时自动创建。
     */
    public YssFilesysUploadFlowResult uploadToDirectory(Path filePath,
                                                        YssFilesysUploadToDirectoryRequest request) throws IOException {
        long chunkSize = resolveChunkSize(request == null ? null : request.getChunkSize());
        String directoryPath = request == null ? null : request.getDirectoryPath();
        String storageSettingId = request == null ? null : request.getStorageSettingId();
        boolean overwriteExisting = resolveOverwriteExisting(request == null ? null : request.getOverwriteExisting());
        return uploadToDirectory(filePath, directoryPath, storageSettingId, chunkSize, overwriteExisting);
    }

    /**
     * 按目录路径上传本地文件，目录不存在时自动创建。
     */
    public YssFilesysUploadFlowResult uploadToDirectory(Path filePath,
                                                        String directoryPath,
                                                        String storageSettingId,
                                                        long chunkSize) throws IOException {
        return uploadToDirectory(filePath, directoryPath, storageSettingId, chunkSize, true);
    }

    /**
     * 按目录路径上传本地文件，目录不存在时自动创建。
     */
    public YssFilesysUploadFlowResult uploadToDirectory(Path filePath,
                                                        String directoryPath,
                                                        String storageSettingId,
                                                        long chunkSize,
                                                        boolean overwriteExisting) throws IOException {
        if (filePath == null) {
            throw new IllegalArgumentException("文件路径不能为空");
        }
        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException("文件不存在: " + filePath);
        }
        byte[] content = Files.readAllBytes(filePath);
        String fileName = filePath.getFileName() == null ? filePath.toString() : filePath.getFileName().toString();
        String mimeType = Files.probeContentType(filePath);
        if (mimeType == null || mimeType.isBlank()) {
            mimeType = "application/octet-stream";
        }
        return uploadToDirectory(content, fileName, mimeType, directoryPath, storageSettingId, chunkSize, overwriteExisting);
    }

    /**
     * 直接上传字节内容到指定父目录。
     *
     * @param content 文件内容
     * @param fileName 文件名
     * @param mimeType MIME 类型
     * @param parentId 父目录ID
     * @param storageSettingId 存储配置ID
     * @param chunkSize 分片大小
     * @return 上传流程结果
     */
    public YssFilesysUploadFlowResult upload(byte[] content,
                                             String fileName,
                                             String mimeType,
                                             String parentId,
                                             String storageSettingId,
                                             long chunkSize) {
        return uploadInternal(content, fileName, mimeType, parentId, storageSettingId, chunkSize, true);
    }

    /**
     * 直接上传字节内容。
     * <p>
     * 当 directoryPath 不为空时，优先按目录路径上传；目录不存在时自动创建。
     * 当 directoryPath 为空时，按 parentId 直接上传。
     * </p>
     *
     * @param content 文件内容
     * @param fileName 文件名
     * @param mimeType MIME 类型
     * @param parentId 父目录ID
     * @param directoryPath 目标目录路径
     * @param storageSettingId 存储配置ID
     * @param chunkSize 分片大小
     * @return 上传流程结果
     */
    public YssFilesysUploadFlowResult upload(byte[] content,
                                             String fileName,
                                             String mimeType,
                                             String parentId,
                                             String directoryPath,
                                             String storageSettingId,
                                             long chunkSize) {
        if (directoryPath != null && !directoryPath.isBlank()) {
            return uploadToDirectory(content, fileName, mimeType, directoryPath, storageSettingId, chunkSize, true);
        }
        return uploadInternal(content, fileName, mimeType, parentId, storageSettingId, chunkSize, true);
    }

    /**
     * 按目录路径上传字节内容，目录不存在时自动创建。
     */
    public YssFilesysUploadFlowResult uploadToDirectory(byte[] content,
                                                        String fileName,
                                                        String mimeType,
                                                        String directoryPath,
                                                        String storageSettingId,
                                                        long chunkSize) {
        return uploadToDirectory(content, fileName, mimeType, directoryPath, storageSettingId, chunkSize, true);
    }

    /**
     * 按目录路径上传字节内容，目录不存在时自动创建。
     */
    public YssFilesysUploadFlowResult uploadToDirectory(byte[] content,
                                                        String fileName,
                                                        String mimeType,
                                                        String directoryPath,
                                                        String storageSettingId,
                                                        long chunkSize,
                                                        boolean overwriteExisting) {
        String parentId = resolveOrCreateDirectoryPath(directoryPath, storageSettingId);
        return uploadInternal(content, fileName, mimeType, parentId, storageSettingId, chunkSize, overwriteExisting);
    }

    /**
     * 按目录路径上传字节内容，目录不存在时自动创建。
     */
    public YssFilesysUploadFlowResult uploadToDirectory(byte[] content,
                                                        String fileName,
                                                        String mimeType,
                                                        YssFilesysUploadToDirectoryRequest request) {
        long chunkSize = resolveChunkSize(request == null ? null : request.getChunkSize());
        String directoryPath = request == null ? null : request.getDirectoryPath();
        String storageSettingId = request == null ? null : request.getStorageSettingId();
        boolean overwriteExisting = resolveOverwriteExisting(request == null ? null : request.getOverwriteExisting());
        return uploadToDirectory(content, fileName, mimeType, directoryPath, storageSettingId, chunkSize, overwriteExisting);
    }

    private YssFilesysUploadFlowResult uploadInternal(byte[] content,
                                                      String fileName,
                                                      String mimeType,
                                                      String parentId,
                                                      String storageSettingId,
                                                      long chunkSize,
                                                      boolean overwriteExisting) {
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
        initRequest.setOverwriteExisting(overwriteExisting);

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

    private String resolveOrCreateDirectoryPath(String directoryPath, String storageSettingId) {
        if (directoryPath == null || directoryPath.isBlank()) {
            return null;
        }
        String normalized = directoryPath.trim().replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.isBlank()) {
            return null;
        }

        String parentId = null;
        for (String folderName : normalized.split("/+")) {
            if (folderName == null || folderName.isBlank()) {
                continue;
            }
            parentId = resolveOrCreateChildDirectory(parentId, folderName.trim(), storageSettingId);
        }
        return parentId;
    }

    private long resolveChunkSize(Long chunkSize) {
        if (chunkSize == null || chunkSize <= 0) {
            return properties.getDefaultChunkSize();
        }
        return chunkSize;
    }

    private boolean resolveOverwriteExisting(Boolean overwriteExisting) {
        return overwriteExisting == null || overwriteExisting;
    }

    private String resolveOrCreateChildDirectory(String parentId, String folderName, String storageSettingId) {
        List<YssFilesysDirectoryTreeDTO> directories = fileSdkService.listDirs(parentId);
        if (directories == null) {
            directories = List.of();
        }
        return directories.stream()
                .filter(item -> Boolean.TRUE.equals(item.getIsDir()))
                .filter(item -> folderName.equals(item.getDisplayName()) || folderName.equals(item.getOriginalName()))
                .map(YssFilesysDirectoryTreeDTO::getFileId)
                .findFirst()
                .orElseGet(() -> createDirectory(parentId, folderName, storageSettingId));
    }

    private String createDirectory(String parentId, String folderName, String storageSettingId) {
        if (folderName == null || folderName.isBlank()) {
            throw new IllegalArgumentException("目录名称不能为空");
        }
        YssFilesysCreateDirectoryRequest request = new YssFilesysCreateDirectoryRequest();
        request.setParentId(parentId);
        request.setFolderName(folderName);
        request.setStorageSettingId(storageSettingId);
        YssFilesysFileRecordDTO record = fileSdkService.createDirectory(request);
        if (record == null || record.getFileId() == null || record.getFileId().isBlank()) {
            throw new IllegalStateException("创建目录失败: " + folderName);
        }
        return record.getFileId();
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
