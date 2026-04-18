package com.yss.filesys.storage.plugin.oss;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.model.AbortMultipartUploadRequest;
import com.aliyun.oss.model.CompleteMultipartUploadRequest;
import com.aliyun.oss.model.CopyObjectRequest;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.InitiateMultipartUploadRequest;
import com.aliyun.oss.model.InitiateMultipartUploadResult;
import com.aliyun.oss.model.ListObjectsRequest;
import com.aliyun.oss.model.ListPartsRequest;
import com.aliyun.oss.model.ObjectListing;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.PartETag;
import com.aliyun.oss.model.PartListing;
import com.aliyun.oss.model.PartSummary;
import com.aliyun.oss.model.UploadPartRequest;
import com.aliyun.oss.model.UploadPartResult;
import com.yss.filesys.application.dto.StorageCapacityDTO;
import com.yss.filesys.domain.model.BizException;
import com.yss.filesys.storage.plugin.core.AbstractStorageOperationService;
import com.yss.filesys.storage.plugin.core.annotation.StoragePlugin;
import com.yss.filesys.storage.plugin.core.config.StorageConfig;
import com.yss.filesys.storage.plugin.core.config.StorageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@StoragePlugin(
        identifier = "OSS",
        name = "阿里云OSS",
        description = "基于阿里云 OSS SDK 的对象存储",
        icon = "cloud",
        configSchema = "{\"bucket\":\"\",\"endpoint\":\"\",\"accessKeyId\":\"\",\"accessKeySecret\":\"\",\"prefix\":\"\"}"
)
public class OSSStorageOperationService extends AbstractStorageOperationService {

    private String bucket;
    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String prefix;
    private OSS ossClient;

    public OSSStorageOperationService() {
        super();
    }

    public OSSStorageOperationService(StorageConfig config) {
        super(config);
    }

    @Override
    protected void validateConfig(StorageConfig config) {
        String bucket = stringProperty(config, "bucket");
        String endpoint = stringProperty(config, "endpoint");
        String accessKeyId = stringProperty(config, "accessKeyId");
        String accessKeySecret = stringProperty(config, "accessKeySecret");
        if (!StringUtils.hasText(bucket)) {
            throw new BizException("OSS bucket 不能为空");
        }
        if (!StringUtils.hasText(endpoint)) {
            throw new BizException("OSS endpoint 不能为空");
        }
        if (!StringUtils.hasText(accessKeyId)) {
            throw new BizException("OSS accessKeyId 不能为空");
        }
        if (!StringUtils.hasText(accessKeySecret)) {
            throw new BizException("OSS accessKeySecret 不能为空");
        }
    }

    @Override
    protected void initialize(StorageConfig config) {
        this.bucket = stringProperty(config, "bucket");
        this.endpoint = normalizeEndpoint(stringProperty(config, "endpoint"));
        this.accessKeyId = stringProperty(config, "accessKeyId");
        this.accessKeySecret = stringProperty(config, "accessKeySecret");
        this.prefix = normalizePrefix(stringProperty(config, "prefix"));
        this.ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }

    @Override
    public void uploadFile(InputStream inputStream, String objectKey) {
        ensureNotPrototype();
        try {
            ossClient.putObject(bucket, resolveObjectKey(objectKey), inputStream);
        } catch (OSSException | ClientException e) {
            throw new BizException("上传文件失败: " + e.getMessage());
        }
    }

    @Override
    public InputStream downloadFile(String objectKey) {
        ensureNotPrototype();
        try {
            OSSObject object = ossClient.getObject(bucket, resolveObjectKey(objectKey));
            return object.getObjectContent();
        } catch (OSSException | ClientException e) {
            throw new BizException("下载文件失败: " + e.getMessage());
        }
    }

    @Override
    public InputStream downloadFileRange(String objectKey, long startByte, long endByte) {
        ensureNotPrototype();
        try {
            GetObjectRequest request = new GetObjectRequest(bucket, resolveObjectKey(objectKey));
            request.setRange(startByte, endByte);
            OSSObject object = ossClient.getObject(request);
            return object.getObjectContent();
        } catch (OSSException | ClientException e) {
            throw new BizException("读取文件分片失败: " + e.getMessage());
        }
    }

    @Override
    public void deleteFile(String objectKey) {
        ensureNotPrototype();
        try {
            ossClient.deleteObject(bucket, resolveObjectKey(objectKey));
        } catch (OSSException | ClientException e) {
            throw new BizException("删除文件失败: " + e.getMessage());
        }
    }

    @Override
    public void rename(String objectKey, String destObjectKey) {
        ensureNotPrototype();
        try {
            String sourceKey = resolveObjectKey(objectKey);
            String targetKey = resolveObjectKey(destObjectKey);
            ossClient.copyObject(new CopyObjectRequest(bucket, sourceKey, bucket, targetKey));
            ossClient.deleteObject(bucket, sourceKey);
        } catch (OSSException | ClientException e) {
            throw new BizException("重命名文件失败: " + e.getMessage());
        }
    }

    @Override
    public String getFileUrl(String objectKey, Integer expireSeconds) {
        ensureNotPrototype();
        int seconds = expireSeconds == null || expireSeconds <= 0 ? 3600 : Math.min(expireSeconds, 604800);
        try {
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, resolveObjectKey(objectKey), HttpMethod.GET);
            request.setExpiration(Date.from(Instant.now().plusSeconds(seconds)));
            return ossClient.generatePresignedUrl(request).toString();
        } catch (OSSException | ClientException e) {
            throw new BizException("生成文件URL失败: " + e.getMessage());
        }
    }

    @Override
    public InputStream getFileStream(String objectKey) {
        return downloadFile(objectKey);
    }

    @Override
    public boolean isFileExist(String objectKey) {
        ensureNotPrototype();
        try {
            return ossClient.doesObjectExist(bucket, resolveObjectKey(objectKey));
        } catch (OSSException | ClientException e) {
            throw new BizException("查询文件失败: " + e.getMessage());
        }
    }

    @Override
    public StorageCapacityDTO getCapacity() {
        ensureNotPrototype();
        long usedBytes = 0L;
        try {
            ListObjectsRequest request = new ListObjectsRequest(bucket);
            if (StringUtils.hasText(prefix)) {
                request.setPrefix(prefix);
            }
            ObjectListing listing;
            do {
                listing = ossClient.listObjects(request);
                for (OSSObjectSummary summary : listing.getObjectSummaries()) {
                    usedBytes += Math.max(0L, summary.getSize());
                }
                request.setMarker(listing.getNextMarker());
            } while (listing.isTruncated());
        } catch (OSSException | ClientException e) {
            throw new BizException("获取 OSS 容量失败: " + e.getMessage());
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
        try {
            InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(bucket, resolveObjectKey(objectKey));
            if (StringUtils.hasText(mimeType)) {
                request.setObjectMetadata(new com.aliyun.oss.model.ObjectMetadata());
                request.getObjectMetadata().setContentType(mimeType);
            }
            InitiateMultipartUploadResult result = ossClient.initiateMultipartUpload(request);
            return result.getUploadId();
        } catch (OSSException | ClientException e) {
            throw new BizException("初始化分片上传失败: " + e.getMessage());
        }
    }

    @Override
    public String uploadPart(String objectKey, String uploadId, int partNumber, long partSize, InputStream partInputStream) {
        ensureNotPrototype();
        try {
            UploadPartRequest request = new UploadPartRequest();
            request.setBucketName(bucket);
            request.setKey(resolveObjectKey(objectKey));
            request.setUploadId(uploadId);
            request.setInputStream(partInputStream);
            request.setPartSize(partSize);
            request.setPartNumber(partNumber);
            UploadPartResult result = ossClient.uploadPart(request);
            return result.getPartETag().getETag();
        } catch (OSSException | ClientException e) {
            throw new BizException("上传分片失败: " + e.getMessage());
        }
    }

    @Override
    public Set<Integer> listParts(String objectKey, String uploadId) {
        ensureNotPrototype();
        try {
            ListPartsRequest request = new ListPartsRequest(bucket, resolveObjectKey(objectKey), uploadId);
            PartListing listing = ossClient.listParts(request);
            Set<Integer> parts = new HashSet<>();
            for (PartSummary partSummary : listing.getParts()) {
                parts.add(partSummary.getPartNumber());
            }
            return parts;
        } catch (OSSException e) {
            if ("NoSuchUpload".equalsIgnoreCase(e.getErrorCode()) || "NoSuchKey".equalsIgnoreCase(e.getErrorCode())) {
                return Set.of();
            }
            throw new BizException("列举分片失败: " + e.getMessage());
        } catch (ClientException e) {
            throw new BizException("列举分片失败: " + e.getMessage());
        }
    }

    @Override
    public void completeMultipartUpload(String objectKey, String uploadId, List<Map<String, Object>> partETags) {
        ensureNotPrototype();
        try {
            List<PartETag> parts = resolveCompletedParts(objectKey, uploadId, partETags);
            if (parts.isEmpty()) {
                throw new BizException("没有可完成的分片");
            }
            parts = parts.stream()
                    .sorted(Comparator.comparingInt(PartETag::getPartNumber))
                    .collect(Collectors.toList());
            ossClient.completeMultipartUpload(new CompleteMultipartUploadRequest(
                    bucket,
                    resolveObjectKey(objectKey),
                    uploadId,
                    parts));
        } catch (OSSException | ClientException e) {
            throw new BizException("完成分片上传失败: " + e.getMessage());
        }
    }

    @Override
    public void abortMultipartUpload(String objectKey, String uploadId) {
        ensureNotPrototype();
        try {
            ossClient.abortMultipartUpload(new AbortMultipartUploadRequest(bucket, resolveObjectKey(objectKey), uploadId));
        } catch (OSSException e) {
            if ("NoSuchUpload".equalsIgnoreCase(e.getErrorCode()) || "NoSuchKey".equalsIgnoreCase(e.getErrorCode())) {
                return;
            }
            throw new BizException("取消分片上传失败: " + e.getMessage());
        } catch (ClientException e) {
            throw new BizException("取消分片上传失败: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        if (ossClient != null) {
            ossClient.shutdown();
        }
    }

    private List<PartETag> resolveCompletedParts(String objectKey, String uploadId, List<Map<String, Object>> partETags) {
        if (partETags != null && !partETags.isEmpty()) {
            return partETags.stream()
                    .map(this::toPartETag)
                    .sorted(Comparator.comparingInt(PartETag::getPartNumber))
                    .collect(Collectors.toList());
        }
        try {
            ListPartsRequest request = new ListPartsRequest(bucket, resolveObjectKey(objectKey), uploadId);
            PartListing listing = ossClient.listParts(request);
            return listing.getParts().stream()
                    .map(partSummary -> new PartETag(partSummary.getPartNumber(), partSummary.getETag()))
                    .sorted(Comparator.comparingInt(PartETag::getPartNumber))
                    .collect(Collectors.toList());
        } catch (OSSException | ClientException e) {
            throw new BizException("读取分片信息失败: " + e.getMessage());
        }
    }

    private PartETag toPartETag(Map<String, Object> partETag) {
        int partNumber = parsePartNumber(partETag.get("partNumber"));
        String eTag = stringValue(partETag.get("eTag"));
        if (!StringUtils.hasText(eTag)) {
            eTag = stringValue(partETag.get("etag"));
        }
        if (!StringUtils.hasText(eTag)) {
            eTag = stringValue(partETag.get("ETag"));
        }
        return new PartETag(partNumber, eTag);
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

    private String stringProperty(StorageConfig config, String key) {
        if (config == null || config.getProperties() == null) {
            return null;
        }
        Object value = config.getProperties().get(key);
        return value == null ? null : value.toString().trim();
    }

    private String normalizeEndpoint(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "https://" + normalized;
        }
        return normalized;
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

    private String resolveObjectKey(String objectKey) {
        String cleanPrefix = prefix == null ? "" : prefix;
        if (!StringUtils.hasText(cleanPrefix)) {
            return objectKey;
        }
        return cleanPrefix + objectKey;
    }

    private String buildStorageRoot() {
        StringBuilder builder = new StringBuilder("oss://").append(bucket);
        if (StringUtils.hasText(prefix)) {
            builder.append('/').append(prefix);
        }
        return builder.toString();
    }
}
