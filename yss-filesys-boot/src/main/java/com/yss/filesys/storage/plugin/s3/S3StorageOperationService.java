package com.yss.filesys.storage.plugin.s3;

import com.yss.filesys.application.dto.StorageCapacityDTO;
import com.yss.filesys.domain.model.BizException;
import com.yss.filesys.storage.plugin.core.AbstractStorageOperationService;
import com.yss.filesys.storage.plugin.core.annotation.StoragePlugin;
import com.yss.filesys.storage.plugin.core.config.StorageConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListPartsRequest;
import software.amazon.awssdk.services.s3.model.ListPartsResponse;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@StoragePlugin(
        identifier = "S3",
        name = "S3存储",
        description = "基于 AWS SDK 的 S3 协议对象存储",
        icon = "cloud",
        configSchema = "{\"bucket\":\"\",\"region\":\"\",\"accessKeyId\":\"\",\"secretAccessKey\":\"\",\"endpoint\":\"\",\"pathStyleEnabled\":true,\"prefix\":\"\"}"
)
public class S3StorageOperationService extends AbstractStorageOperationService {

    private String bucket;
    private String region;
    private String accessKeyId;
    private String secretAccessKey;
    private String endpoint;
    private boolean pathStyleEnabled;
    private String prefix;
    private S3Client s3Client;
    private S3Presigner s3Presigner;

    @Autowired
    public S3StorageOperationService() {
        super();
    }

    public S3StorageOperationService(StorageConfig config) {
        super(config);
    }

    @Override
    protected void validateConfig(StorageConfig config) {
        String bucket = stringProperty(config, "bucket");
        String region = stringProperty(config, "region");
        String accessKeyId = stringProperty(config, "accessKeyId");
        String secretAccessKey = stringProperty(config, "secretAccessKey");
        if (!StringUtils.hasText(bucket)) {
            throw new BizException("S3 bucket 不能为空");
        }
        if (!StringUtils.hasText(region)) {
            throw new BizException("S3 region 不能为空");
        }
        if (!StringUtils.hasText(accessKeyId)) {
            throw new BizException("S3 accessKeyId 不能为空");
        }
        if (!StringUtils.hasText(secretAccessKey)) {
            throw new BizException("S3 secretAccessKey 不能为空");
        }
    }

    @Override
    protected void initialize(StorageConfig config) {
        this.bucket = stringProperty(config, "bucket");
        this.region = stringProperty(config, "region");
        this.accessKeyId = stringProperty(config, "accessKeyId");
        this.secretAccessKey = stringProperty(config, "secretAccessKey");
        this.endpoint = stringProperty(config, "endpoint");
        this.pathStyleEnabled = booleanProperty(config, "pathStyleEnabled", true);
        this.prefix = normalizePrefix(stringProperty(config, "prefix"));
        this.s3Client = buildClient();
        this.s3Presigner = buildPresigner();
    }

    @Override
    public void uploadFile(InputStream inputStream, String objectKey) {
        ensureNotPrototype();
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("yss-s3-upload-", ".tmp");
            Files.copy(inputStream, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            s3Client.putObject(PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(resolveObjectKey(objectKey))
                            .build(),
                    RequestBody.fromFile(tempFile));
        } catch (IOException e) {
            throw new BizException("上传文件失败: " + e.getMessage());
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                }
            }
        }
    }

    @Override
    public InputStream downloadFile(String objectKey) {
        ensureNotPrototype();
        ResponseInputStream<GetObjectResponse> response = s3Client.getObject(GetObjectRequest.builder()
                .bucket(bucket)
                .key(resolveObjectKey(objectKey))
                .build());
        return response;
    }

    @Override
    public InputStream downloadFileRange(String objectKey, long startByte, long endByte) {
        ensureNotPrototype();
        ResponseInputStream<GetObjectResponse> response = s3Client.getObject(GetObjectRequest.builder()
                .bucket(bucket)
                .key(resolveObjectKey(objectKey))
                .range("bytes=" + startByte + "-" + endByte)
                .build());
        return response;
    }

    @Override
    public void deleteFile(String objectKey) {
        ensureNotPrototype();
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(resolveObjectKey(objectKey))
                .build());
    }

    @Override
    public void rename(String objectKey, String destObjectKey) {
        ensureNotPrototype();
        String sourceKey = resolveObjectKey(objectKey);
        String targetKey = resolveObjectKey(destObjectKey);
        s3Client.copyObject(CopyObjectRequest.builder()
                .sourceBucket(bucket)
                .sourceKey(sourceKey)
                .destinationBucket(bucket)
                .destinationKey(targetKey)
                .build());
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(sourceKey)
                .build());
    }

    @Override
    public String getFileUrl(String objectKey, Integer expireSeconds) {
        ensureNotPrototype();
        int seconds = expireSeconds == null || expireSeconds <= 0 ? 3600 : Math.min(expireSeconds, 604800);
        return s3Presigner.presignGetObject(GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(seconds))
                .getObjectRequest(GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(resolveObjectKey(objectKey))
                        .build())
                .build())
                .url()
                .toString();
    }

    @Override
    public InputStream getFileStream(String objectKey) {
        return downloadFile(objectKey);
    }

    @Override
    public boolean isFileExist(String objectKey) {
        ensureNotPrototype();
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(resolveObjectKey(objectKey))
                    .build());
            return true;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            throw new BizException("查询文件失败: " + e.getMessage());
        }
    }

    @Override
    public StorageCapacityDTO getCapacity() {
        ensureNotPrototype();
        long usedBytes = 0L;
        try {
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .prefix(prefix)
                    .build();
            for (var page : s3Client.listObjectsV2Paginator(request)) {
                for (S3Object object : page.contents()) {
                    usedBytes += Math.max(0L, object.size());
                }
            }
        } catch (S3Exception e) {
            throw new BizException("获取 S3 容量失败: " + e.getMessage());
        }
        return StorageCapacityDTO.builder()
                .settingId(config.getConfigId())
                .platformIdentifier(config.getPlatformIdentifier())
                .totalBytes(0L)
                .usedBytes(usedBytes)
                .freeBytes(0L)
                .usableBytes(0L)
                .storageRoot(buildStorageRoot())
                .build();
    }

    @Override
    public String initiateMultipartUpload(String objectKey, String mimeType) {
        ensureNotPrototype();
        CreateMultipartUploadResponse response = s3Client.createMultipartUpload(CreateMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(resolveObjectKey(objectKey))
                .contentType(StringUtils.hasText(mimeType) ? mimeType : "application/octet-stream")
                .build());
        return response.uploadId();
    }

    @Override
    public String uploadPart(String objectKey, String uploadId, int partNumber, long partSize, InputStream partInputStream) {
        ensureNotPrototype();
        UploadPartResponse response = s3Client.uploadPart(UploadPartRequest.builder()
                        .bucket(bucket)
                        .key(resolveObjectKey(objectKey))
                        .uploadId(uploadId)
                        .partNumber(partNumber)
                        .contentLength(partSize)
                        .build(),
                RequestBody.fromInputStream(partInputStream, partSize));
        return response.eTag();
    }

    @Override
    public Set<Integer> listParts(String objectKey, String uploadId) {
        ensureNotPrototype();
        try {
            ListPartsRequest request = ListPartsRequest.builder()
                    .bucket(bucket)
                    .key(resolveObjectKey(objectKey))
                    .uploadId(uploadId)
                    .build();
            Set<Integer> parts = new HashSet<>();
            for (ListPartsResponse response : s3Client.listPartsPaginator(request)) {
                response.parts().forEach(part -> parts.add(part.partNumber()));
            }
            return parts;
        } catch (S3Exception e) {
            if ("NoSuchUpload".equalsIgnoreCase(e.awsErrorDetails() == null ? null : e.awsErrorDetails().errorCode())
                    || e.statusCode() == 404) {
                return Set.of();
            }
            throw new BizException("列举分片失败: " + e.getMessage());
        }
    }

    @Override
    public void completeMultipartUpload(String objectKey, String uploadId, List<Map<String, Object>> partETags) {
        ensureNotPrototype();
        try {
            List<CompletedPart> completedParts = resolveCompletedParts(objectKey, uploadId, partETags);
            if (completedParts.isEmpty()) {
                throw new BizException("没有可完成的分片");
            }
            s3Client.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
                    .bucket(bucket)
                    .key(resolveObjectKey(objectKey))
                    .uploadId(uploadId)
                    .multipartUpload(CompletedMultipartUpload.builder()
                            .parts(completedParts)
                            .build())
                    .build());
        } catch (S3Exception e) {
            throw new BizException("完成分片上传失败: " + e.getMessage());
        }
    }

    @Override
    public void abortMultipartUpload(String objectKey, String uploadId) {
        ensureNotPrototype();
        try {
            s3Client.abortMultipartUpload(AbortMultipartUploadRequest.builder()
                    .bucket(bucket)
                    .key(resolveObjectKey(objectKey))
                    .uploadId(uploadId)
                    .build());
        } catch (S3Exception e) {
            if ("NoSuchUpload".equalsIgnoreCase(e.awsErrorDetails() == null ? null : e.awsErrorDetails().errorCode())
                    || e.statusCode() == 404) {
                return;
            }
            throw new BizException("取消分片上传失败: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        if (s3Client != null) {
            s3Client.close();
        }
        if (s3Presigner != null) {
            s3Presigner.close();
        }
    }

    private S3Client buildClient() {
        var builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .serviceConfiguration(software.amazon.awssdk.services.s3.S3Configuration.builder()
                        .pathStyleAccessEnabled(pathStyleEnabled)
                        .build());
        if (StringUtils.hasText(endpoint)) {
            builder.endpointOverride(URI.create(endpoint));
        }
        return builder.build();
    }

    private S3Presigner buildPresigner() {
        var builder = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)));
        if (StringUtils.hasText(endpoint)) {
            builder.endpointOverride(URI.create(endpoint));
        }
        return builder.build();
    }

    private String resolveObjectKey(String objectKey) {
        String cleanPrefix = prefix == null ? "" : prefix;
        if (!StringUtils.hasText(cleanPrefix)) {
            return objectKey;
        }
        return cleanPrefix + objectKey;
    }

    private String buildStorageRoot() {
        StringBuilder builder = new StringBuilder("s3://").append(bucket);
        if (StringUtils.hasText(prefix)) {
            builder.append('/').append(prefix);
        }
        return builder.toString();
    }

    private String normalizePrefix(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.trim();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (!normalized.endsWith("/")) {
            normalized = normalized + "/";
        }
        return normalized;
    }

    private String stringProperty(StorageConfig config, String key) {
        if (config == null || config.getProperties() == null) {
            return null;
        }
        Object value = config.getProperties().get(key);
        return value == null ? null : value.toString().trim();
    }

    private boolean booleanProperty(StorageConfig config, String key, boolean defaultValue) {
        if (config == null || config.getProperties() == null) {
            return defaultValue;
        }
        Object value = config.getProperties().get(key);
        return value == null ? defaultValue : Boolean.parseBoolean(value.toString());
    }

    private List<CompletedPart> resolveCompletedParts(String objectKey, String uploadId, List<Map<String, Object>> partETags) {
        if (partETags != null && !partETags.isEmpty()) {
            return partETags.stream()
                    .map(this::toCompletedPart)
                    .sorted(Comparator.comparingInt(CompletedPart::partNumber))
                    .collect(Collectors.toList());
        }
        try {
            ListPartsRequest request = ListPartsRequest.builder()
                    .bucket(bucket)
                    .key(resolveObjectKey(objectKey))
                    .uploadId(uploadId)
                    .build();
            List<CompletedPart> parts = new ArrayList<>();
            for (ListPartsResponse response : s3Client.listPartsPaginator(request)) {
                response.parts().forEach(part -> parts.add(CompletedPart.builder()
                        .partNumber(part.partNumber())
                        .eTag(part.eTag())
                        .build()));
            }
            return parts.stream()
                    .sorted(Comparator.comparingInt(CompletedPart::partNumber))
                    .collect(Collectors.toList());
        } catch (S3Exception e) {
            throw new BizException("读取分片信息失败: " + e.getMessage());
        }
    }

    private CompletedPart toCompletedPart(Map<String, Object> partETag) {
        int partNumber = parsePartNumber(partETag.get("partNumber"));
        String eTag = stringValue(partETag.get("eTag"));
        if (!StringUtils.hasText(eTag)) {
            eTag = stringValue(partETag.get("etag"));
        }
        if (!StringUtils.hasText(eTag)) {
            eTag = stringValue(partETag.get("ETag"));
        }
        return CompletedPart.builder()
                .partNumber(partNumber)
                .eTag(eTag)
                .build();
    }

    private int parsePartNumber(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            return Integer.parseInt(value.toString());
        }
        throw new BizException("分片编号不能为空");
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }
}
