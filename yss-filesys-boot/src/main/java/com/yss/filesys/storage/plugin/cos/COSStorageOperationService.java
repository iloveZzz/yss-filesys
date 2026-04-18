package com.yss.filesys.storage.plugin.cos;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.AbortMultipartUploadRequest;
import com.qcloud.cos.model.CompleteMultipartUploadRequest;
import com.qcloud.cos.model.CopyObjectRequest;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.InitiateMultipartUploadRequest;
import com.qcloud.cos.model.InitiateMultipartUploadResult;
import com.qcloud.cos.model.ListObjectsRequest;
import com.qcloud.cos.model.ListPartsRequest;
import com.qcloud.cos.model.ObjectListing;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectSummary;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PartETag;
import com.qcloud.cos.model.PartListing;
import com.qcloud.cos.model.PartSummary;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.UploadPartRequest;
import com.qcloud.cos.model.UploadPartResult;
import com.qcloud.cos.region.Region;
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
import java.net.URL;
import java.time.Instant;
import java.util.Date;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@StoragePlugin(
        identifier = "COS",
        name = "腾讯云COS",
        description = "基于腾讯云 COS SDK 的对象存储",
        icon = "cloud",
        configSchema = "{\"bucket\":\"\",\"region\":\"\",\"secretId\":\"\",\"secretKey\":\"\",\"endpoint\":\"\",\"prefix\":\"\"}"
)
public class COSStorageOperationService extends AbstractStorageOperationService {

    private String bucket;
    private String region;
    private String secretId;
    private String secretKey;
    private String endpoint;
    private String prefix;
    private COSClient cosClient;

    public COSStorageOperationService() {
        super();
    }

    public COSStorageOperationService(StorageConfig config) {
        super(config);
    }

    @Override
    protected void validateConfig(StorageConfig config) {
        String bucket = stringProperty(config, "bucket");
        String region = stringProperty(config, "region");
        String secretId = stringProperty(config, "secretId");
        String secretKey = stringProperty(config, "secretKey");
        if (!StringUtils.hasText(bucket)) {
            throw new BizException("COS bucket 不能为空");
        }
        if (!StringUtils.hasText(region)) {
            throw new BizException("COS region 不能为空");
        }
        if (!StringUtils.hasText(secretId)) {
            throw new BizException("COS secretId 不能为空");
        }
        if (!StringUtils.hasText(secretKey)) {
            throw new BizException("COS secretKey 不能为空");
        }
    }

    @Override
    protected void initialize(StorageConfig config) {
        this.bucket = stringProperty(config, "bucket");
        this.region = stringProperty(config, "region");
        this.secretId = stringProperty(config, "secretId");
        this.secretKey = stringProperty(config, "secretKey");
        this.endpoint = normalizeEndpoint(stringProperty(config, "endpoint"));
        this.prefix = normalizePrefix(stringProperty(config, "prefix"));
        this.cosClient = buildClient();
    }

    @Override
    public void uploadFile(InputStream inputStream, String objectKey) {
        ensureNotPrototype();
        try {
            PutObjectRequest request = new PutObjectRequest(bucket, resolveObjectKey(objectKey), inputStream, new ObjectMetadata());
            cosClient.putObject(request);
        } catch (CosClientException e) {
            throw new BizException("上传文件失败: " + e.getMessage());
        }
    }

    @Override
    public InputStream downloadFile(String objectKey) {
        ensureNotPrototype();
        try {
            COSObject object = cosClient.getObject(bucket, resolveObjectKey(objectKey));
            return object.getObjectContent();
        } catch (CosClientException e) {
            throw new BizException("下载文件失败: " + e.getMessage());
        }
    }

    @Override
    public InputStream downloadFileRange(String objectKey, long startByte, long endByte) {
        ensureNotPrototype();
        try {
            GetObjectRequest request = new GetObjectRequest(bucket, resolveObjectKey(objectKey));
            request.setRange(startByte, endByte);
            COSObject object = cosClient.getObject(request);
            return object.getObjectContent();
        } catch (CosClientException e) {
            throw new BizException("读取文件分片失败: " + e.getMessage());
        }
    }

    @Override
    public void deleteFile(String objectKey) {
        ensureNotPrototype();
        try {
            cosClient.deleteObject(bucket, resolveObjectKey(objectKey));
        } catch (CosClientException e) {
            throw new BizException("删除文件失败: " + e.getMessage());
        }
    }

    @Override
    public void rename(String objectKey, String destObjectKey) {
        ensureNotPrototype();
        try {
            String sourceKey = resolveObjectKey(objectKey);
            String targetKey = resolveObjectKey(destObjectKey);
            cosClient.copyObject(new CopyObjectRequest(bucket, sourceKey, bucket, targetKey));
            cosClient.deleteObject(bucket, sourceKey);
        } catch (CosClientException e) {
            throw new BizException("重命名文件失败: " + e.getMessage());
        }
    }

    @Override
    public String getFileUrl(String objectKey, Integer expireSeconds) {
        ensureNotPrototype();
        int seconds = expireSeconds == null || expireSeconds <= 0 ? 3600 : Math.min(expireSeconds, 604800);
        try {
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, resolveObjectKey(objectKey), HttpMethodName.GET);
            request.setExpiration(Date.from(Instant.now().plusSeconds(seconds)));
            URL url = cosClient.generatePresignedUrl(request);
            return url == null ? null : url.toString();
        } catch (CosClientException e) {
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
            return cosClient.doesObjectExist(bucket, resolveObjectKey(objectKey));
        } catch (CosClientException e) {
            throw new BizException("查询文件失败: " + e.getMessage());
        }
    }

    @Override
    public StorageCapacityDTO getCapacity() {
        ensureNotPrototype();
        long usedBytes = 0L;
        try {
            ListObjectsRequest request = new ListObjectsRequest();
            request.setBucketName(bucket);
            if (StringUtils.hasText(prefix)) {
                request.setPrefix(prefix);
            }
            ObjectListing listing;
            do {
                listing = cosClient.listObjects(request);
                if (listing.getObjectSummaries() != null) {
                    for (COSObjectSummary summary : listing.getObjectSummaries()) {
                        usedBytes += Math.max(0L, summary.getSize());
                    }
                }
                request.setMarker(listing.getNextMarker());
            } while (listing.isTruncated());
        } catch (CosClientException e) {
            throw new BizException("获取 COS 容量失败: " + e.getMessage());
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
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentType(mimeType);
                request.setObjectMetadata(metadata);
            }
            InitiateMultipartUploadResult result = cosClient.initiateMultipartUpload(request);
            return result.getUploadId();
        } catch (CosClientException e) {
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
            UploadPartResult result = cosClient.uploadPart(request);
            return result.getPartETag().getETag();
        } catch (CosClientException e) {
            throw new BizException("上传分片失败: " + e.getMessage());
        }
    }

    @Override
    public Set<Integer> listParts(String objectKey, String uploadId) {
        ensureNotPrototype();
        try {
            ListPartsRequest request = new ListPartsRequest(bucket, resolveObjectKey(objectKey), uploadId);
            PartListing listing = cosClient.listParts(request);
            Set<Integer> parts = new HashSet<>();
            if (listing.getParts() != null) {
                for (PartSummary summary : listing.getParts()) {
                    parts.add(summary.getPartNumber());
                }
            }
            return parts;
        } catch (CosClientException e) {
            if (e instanceof CosServiceException service && "NoSuchUpload".equalsIgnoreCase(service.getErrorCode())) {
                return Set.of();
            }
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
            cosClient.completeMultipartUpload(new CompleteMultipartUploadRequest(
                    bucket,
                    resolveObjectKey(objectKey),
                    uploadId,
                    parts));
        } catch (CosClientException e) {
            throw new BizException("完成分片上传失败: " + e.getMessage());
        }
    }

    @Override
    public void abortMultipartUpload(String objectKey, String uploadId) {
        ensureNotPrototype();
        try {
            cosClient.abortMultipartUpload(new AbortMultipartUploadRequest(bucket, resolveObjectKey(objectKey), uploadId));
        } catch (CosClientException e) {
            if (e instanceof CosServiceException service && "NoSuchUpload".equalsIgnoreCase(service.getErrorCode())) {
                return;
            }
            throw new BizException("取消分片上传失败: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        if (cosClient != null) {
            cosClient.shutdown();
        }
    }

    private COSClient buildClient() {
        COSCredentials credentials = new BasicCOSCredentials(secretId, secretKey);
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        if (StringUtils.hasText(endpoint)) {
            clientConfig.setEndPointSuffix(extractEndpointSuffix(endpoint));
        }
        clientConfig.setHttpProtocol(com.qcloud.cos.http.HttpProtocol.https);
        return new COSClient(credentials, clientConfig);
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
            PartListing listing = cosClient.listParts(request);
            if (listing.getParts() == null) {
                return List.of();
            }
            return listing.getParts().stream()
                    .map(partSummary -> new PartETag(partSummary.getPartNumber(), partSummary.getETag()))
                    .sorted(Comparator.comparingInt(PartETag::getPartNumber))
                    .collect(Collectors.toList());
        } catch (CosClientException e) {
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

    private String extractEndpointSuffix(String value) {
        String normalized = normalizeEndpoint(value);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        return java.net.URI.create(normalized).getHost();
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
        StringBuilder builder = new StringBuilder("cos://").append(bucket);
        if (StringUtils.hasText(prefix)) {
            builder.append('/').append(prefix);
        }
        return builder.toString();
    }
}
