package com.yss.filesys.preview;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PreviewTokenStore {

    private final long expireSeconds;
    private final Map<String, TokenRecord> cache = new ConcurrentHashMap<>();

    public PreviewTokenStore(@Value("${yss.preview.token-expire-seconds:300}") long expireSeconds) {
        this.expireSeconds = expireSeconds;
    }

    public String issue(String fileId) {
        return issueForResource("file:" + fileId);
    }

    public String issueForArchive(String archiveFileId, String innerPath) {
        return issueForResource("archive:" + archiveFileId + ":" + innerPath);
    }

    public boolean verifyForArchive(String token, String archiveFileId, String innerPath) {
        return verifyForResource(token, "archive:" + archiveFileId + ":" + innerPath);
    }

    public String issueForResource(String resourceKey) {
        cleanup();
        String token = UUID.randomUUID().toString().replace("-", "");
        cache.put(token, new TokenRecord(resourceKey, Instant.now().plusSeconds(expireSeconds)));
        return token;
    }

    public boolean verify(String token, String fileId) {
        return verifyForResource(token, "file:" + fileId);
    }

    public boolean verifyForResource(String token, String resourceKey) {
        cleanup();
        TokenRecord record = cache.get(token);
        if (record == null) {
            return false;
        }
        return record.resourceKey().equals(resourceKey) && Instant.now().isBefore(record.expiredAt());
    }

    private void cleanup() {
        Instant now = Instant.now();
        cache.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiredAt()));
    }

    private record TokenRecord(String resourceKey, Instant expiredAt) {
    }
}
