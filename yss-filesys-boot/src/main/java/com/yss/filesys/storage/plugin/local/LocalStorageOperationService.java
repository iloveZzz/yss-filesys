package com.yss.filesys.storage.plugin.local;

import com.yss.filesys.domain.model.BizException;
import com.yss.filesys.storage.plugin.core.AbstractStorageOperationService;
import com.yss.filesys.storage.plugin.core.annotation.StoragePlugin;
import com.yss.filesys.storage.plugin.core.config.StorageConfig;
import com.yss.filesys.storage.plugin.core.config.StorageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@StoragePlugin(
        identifier = StorageUtils.LOCAL_PLATFORM_IDENTIFIER,
        name = "本地存储",
        description = "本地文件系统存储",
        icon = "folder",
        isDefault = true,
        configSchema = "{\"storageRoot\":\"/tmp/yss-filesys/storage\"}"
)
public class LocalStorageOperationService extends AbstractStorageOperationService {

    private String storageRoot;

    public LocalStorageOperationService(@Value("${yss.files.storage-root:/tmp/yss-filesys/storage}") String defaultStorageRoot) {
        super(StorageConfig.builder()
                .configId(null)
                .platformIdentifier(StorageUtils.LOCAL_PLATFORM_IDENTIFIER)
                .userId(null)
                .properties(Map.of("storageRoot", defaultStorageRoot))
                .enabled(true)
                .remark("default local storage")
                .build());
    }

    public LocalStorageOperationService(StorageConfig config) {
        super(config);
    }

    @Override
    protected void validateConfig(StorageConfig config) {
        if (config != null && config.getProperties() != null && config.getProperties().containsKey("storageRoot")) {
            Object root = config.getProperties().get("storageRoot");
            if (root == null || root.toString().isBlank()) {
                throw new BizException("本地存储 storageRoot 不能为空");
            }
        }
    }

    @Override
    protected void initialize(StorageConfig config) {
        this.storageRoot = resolveStorageRoot(config);
    }

    @Override
    public void uploadFile(InputStream inputStream, String objectKey) {
        ensureNotPrototype();
        try {
            Path path = resolveObjectPath(objectKey);
            Files.createDirectories(path.getParent());
            Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BizException("上传文件失败: " + e.getMessage());
        }
    }

    @Override
    public InputStream downloadFile(String objectKey) {
        ensureNotPrototype();
        try {
            return Files.newInputStream(resolveObjectPath(objectKey));
        } catch (IOException e) {
            throw new BizException("下载文件失败: " + e.getMessage());
        }
    }

    @Override
    public InputStream downloadFileRange(String objectKey, long startByte, long endByte) {
        ensureNotPrototype();
        try {
            Path path = resolveObjectPath(objectKey);
            RandomAccessFile raf = new RandomAccessFile(path.toFile(), "r");
            raf.seek(startByte);
            long length = endByte - startByte + 1;
            byte[] bytes = new byte[(int) length];
            raf.readFully(bytes);
            raf.close();
            return new java.io.ByteArrayInputStream(bytes);
        } catch (IOException e) {
            throw new BizException("读取文件分片失败: " + e.getMessage());
        }
    }

    @Override
    public void deleteFile(String objectKey) {
        ensureNotPrototype();
        try {
            Files.deleteIfExists(resolveObjectPath(objectKey));
        } catch (IOException e) {
            throw new BizException("删除文件失败: " + e.getMessage());
        }
    }

    @Override
    public void rename(String objectKey, String destObjectKey) {
        ensureNotPrototype();
        try {
            Path source = resolveObjectPath(objectKey);
            Path target = resolveObjectPath(destObjectKey);
            Files.createDirectories(target.getParent());
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BizException("重命名文件失败: " + e.getMessage());
        }
    }

    @Override
    public String getFileUrl(String objectKey, Integer expireSeconds) {
        return resolveObjectPath(objectKey).toUri().toString();
    }

    @Override
    public InputStream getFileStream(String objectKey) {
        return downloadFile(objectKey);
    }

    @Override
    public boolean isFileExist(String objectKey) {
        return Files.exists(resolveObjectPath(objectKey));
    }

    @Override
    public String initiateMultipartUpload(String objectKey, String mimeType) {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public String uploadPart(String objectKey, String uploadId, int partNumber, long partSize, InputStream partInputStream) {
        try {
            Path partPath = resolveChunkPath(uploadId, partNumber);
            Files.createDirectories(partPath.getParent());
            Files.copy(partInputStream, partPath, StandardCopyOption.REPLACE_EXISTING);
            return String.valueOf(Files.size(partPath));
        } catch (IOException e) {
            throw new BizException("上传分片失败: " + e.getMessage());
        }
    }

    @Override
    public Set<Integer> listParts(String objectKey, String uploadId) {
        Path dir = chunkDir(uploadId);
        if (!Files.exists(dir)) {
            return Set.of();
        }
        try (var stream = Files.list(dir)) {
            Set<Integer> parts = new HashSet<>();
            stream.filter(p -> p.getFileName().toString().endsWith(".part")).forEach(path -> {
                String name = path.getFileName().toString().replace(".part", "");
                try {
                    parts.add(Integer.parseInt(name));
                } catch (NumberFormatException ignored) {
                }
            });
            return parts;
        } catch (IOException e) {
            throw new BizException("列举分片失败: " + e.getMessage());
        }
    }

    @Override
    public void completeMultipartUpload(String objectKey, String uploadId, List<Map<String, Object>> partETags) {
        try {
            Path target = resolveObjectPath(objectKey);
            Files.createDirectories(target.getParent());
            Files.deleteIfExists(target);
            Files.createFile(target);
            for (int i = 0; i < partETags.size(); i++) {
                Path part = resolveChunkPath(uploadId, i);
                if (!Files.exists(part)) {
                    throw new BizException("分片缺失: " + i);
                }
                Files.write(target, Files.readAllBytes(part), java.nio.file.StandardOpenOption.APPEND);
            }
        } catch (IOException e) {
            throw new BizException("完成分片上传失败: " + e.getMessage());
        }
    }

    @Override
    public void abortMultipartUpload(String objectKey, String uploadId) {
        try {
            Path dir = chunkDir(uploadId);
            if (!Files.exists(dir)) {
                return;
            }
            try (var stream = Files.walk(dir)) {
                stream.sorted((a, b) -> b.compareTo(a)).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                    }
                });
            }
        } catch (IOException e) {
            throw new BizException("取消分片上传失败: " + e.getMessage());
        }
    }

    private String resolveStorageRoot(StorageConfig config) {
        if (config != null && config.getProperties() != null && config.getProperties().get("storageRoot") != null) {
            return config.getProperties().get("storageRoot").toString();
        }
        return "/tmp/yss-filesys/storage";
    }

    private Path resolveObjectPath(String objectKey) {
        return Path.of(storageRoot, "files", objectKey);
    }

    private Path chunkDir(String uploadId) {
        return Path.of(storageRoot, "chunks", uploadId);
    }

    private Path resolveChunkPath(String uploadId, int partNumber) {
        return chunkDir(uploadId).resolve(partNumber + ".part");
    }
}
