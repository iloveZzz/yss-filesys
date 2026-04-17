package com.yss.filesys.storage.plugin.core;

import com.yss.filesys.storage.plugin.core.config.StorageConfig;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface IStorageOperationService extends Closeable {

    IStorageOperationService createConfiguredInstance(StorageConfig config);

    void uploadFile(InputStream inputStream, String objectKey);

    InputStream downloadFile(String objectKey);

    InputStream downloadFileRange(String objectKey, long startByte, long endByte);

    void deleteFile(String objectKey);

    void rename(String objectKey, String destObjectKey);

    String getFileUrl(String objectKey, Integer expireSeconds);

    InputStream getFileStream(String objectKey);

    boolean isFileExist(String objectKey);

    String initiateMultipartUpload(String objectKey, String mimeType);

    String uploadPart(String objectKey, String uploadId, int partNumber, long partSize, InputStream partInputStream);

    Set<Integer> listParts(String objectKey, String uploadId);

    void completeMultipartUpload(String objectKey, String uploadId, List<Map<String, Object>> partETags);

    void abortMultipartUpload(String objectKey, String uploadId);

    @Override
    default void close() throws IOException {
    }
}
