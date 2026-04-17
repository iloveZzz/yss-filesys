package com.yss.filesys.schedule;

import com.yss.filesys.domain.gateway.FileTransferTaskGateway;
import com.yss.filesys.domain.model.FileTransferTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransferTaskCleanupJob {

    private final FileTransferTaskGateway fileTransferTaskGateway;

    @Value("${yss.files.storage-root:/tmp/yss-filesys/storage}")
    private String storageRoot;

    @Scheduled(cron = "0 15 0 * * ?")
    public void cleanupExpiredTasks() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        List<FileTransferTask> tasks = fileTransferTaskGateway.listFinishedBefore(cutoff);
        if (tasks.isEmpty()) {
            return;
        }
        fileTransferTaskGateway.deleteByTaskIds(tasks.stream().map(FileTransferTask::getTaskId).toList());
        for (FileTransferTask task : tasks) {
            deleteChunkDir(task.getTaskId());
        }
        log.info("cleaned expired transfer tasks: {}", tasks.size());
    }

    private void deleteChunkDir(String taskId) {
        Path dir = Path.of(storageRoot, "chunks", taskId);
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
        } catch (IOException e) {
            log.warn("failed to cleanup chunk dir {}: {}", taskId, e.getMessage());
        }
    }
}
