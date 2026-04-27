package com.yss.filesys.application.service;

import com.yss.filesys.domain.model.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class UploadDestinationReservationService {

    private static final long RESERVATION_TTL_MILLIS = TimeUnit.HOURS.toMillis(2);
    private static final long RELEASE_DELAY_MILLIS = TimeUnit.SECONDS.toMillis(1);
    private static final long CLEANUP_INTERVAL_MILLIS = TimeUnit.MINUTES.toMillis(1);

    private final ConcurrentMap<String, Reservation> reservations = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> taskToKey = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Object> keyLocks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "upload-destination-reservation-cleaner");
        thread.setDaemon(true);
        return thread;
    });

    public UploadDestinationReservationService() {
        cleaner.scheduleWithFixedDelay(this::cleanupExpiredReservations,
                CLEANUP_INTERVAL_MILLIS,
                CLEANUP_INTERVAL_MILLIS,
                TimeUnit.MILLISECONDS);
    }

    public void reserve(String taskId, String userId, String parentId, String fileName) {
        String key = buildKey(userId, parentId, fileName);
        synchronized (lockFor(key)) {
            long now = currentTimeMillis();
            Reservation existing = reservations.get(key);
            if (existing != null && !existing.isExpired(now) && !taskId.equals(existing.taskId)) {
                log.warn("目标目录已存在同名文件正在上传，请稍后重试");
            }
            Reservation reservation = new Reservation(taskId, key, now, now + RESERVATION_TTL_MILLIS);
            reservations.put(key, reservation);
            taskToKey.put(taskId, key);
        }
    }

    public void touch(String taskId) {
        String key = taskToKey.get(taskId);
        if (key == null || key.isBlank()) {
            return;
        }
        synchronized (lockFor(key)) {
            long now = currentTimeMillis();
            Reservation existing = reservations.get(key);
            if (existing == null || !taskId.equals(existing.taskId)) {
                return;
            }
            reservations.put(key, existing.refresh(now + RESERVATION_TTL_MILLIS));
        }
    }

    public void release(String taskId) {
        String key = taskToKey.remove(taskId);
        if (key == null || key.isBlank()) {
            return;
        }
        cleaner.schedule(() -> releaseIfOwned(taskId, key), RELEASE_DELAY_MILLIS, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    public void shutdown() {
        cleaner.shutdownNow();
    }

    private void releaseIfOwned(String taskId, String key) {
        synchronized (lockFor(key)) {
            Reservation existing = reservations.get(key);
            if (existing == null || !taskId.equals(existing.taskId)) {
                return;
            }
            reservations.remove(key);
        }
    }

    private void cleanupExpiredReservations() {
        long now = currentTimeMillis();
        reservations.forEach((key, reservation) -> {
            if (reservation.isExpired(now)) {
                synchronized (lockFor(key)) {
                    Reservation current = reservations.get(key);
                    if (current != null && current.isExpired(now)) {
                        reservations.remove(key);
                        taskToKey.remove(current.taskId, key);
                    }
                }
            }
        });
    }

    private Object lockFor(String key) {
        return keyLocks.computeIfAbsent(key, ignored -> new Object());
    }

    private String buildKey(String userId, String parentId, String fileName) {
        String normalizedUserId = userId == null ? "" : userId.trim();
        String normalizedParentId = parentId == null ? "" : parentId.trim();
        String normalizedFileName = fileName == null ? "" : fileName.trim();
        return normalizedUserId + "|" + normalizedParentId + "|" + normalizedFileName;
    }

    private long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    private record Reservation(String taskId, String key, long reservedAt, long expiresAt) {
        private boolean isExpired(long now) {
            return now >= expiresAt;
        }

        private Reservation refresh(long newExpiresAt) {
            return new Reservation(taskId, key, reservedAt, newExpiresAt);
        }
    }
}
