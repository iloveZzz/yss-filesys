package com.yss.filesys.service.impl;

import com.yss.filesys.service.TransferSseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class TransferSseServiceImpl implements TransferSseService {

    private static final Duration COMPLETE_EVENT_TTL = Duration.ofDays(7);
    private static final Duration HEARTBEAT_INTERVAL = Duration.ofSeconds(15);

    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final Map<SseEmitter, ScheduledFuture<?>> heartbeatTasks = new ConcurrentHashMap<>();
    private final Map<String, Map<String, CachedCompleteEvent>> completeEventCache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "yss-filesys-sse-heartbeat");
        thread.setDaemon(true);
        return thread;
    });

    @Override
    public SseEmitter subscribe(String userId) {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.computeIfAbsent(userId, key -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError(throwable -> removeEmitter(userId, emitter));
        scheduleHeartbeat(userId, emitter);
        replayCachedCompleteEvents(userId, emitter);
        return emitter;
    }

    @Override
    public void sendStatus(String userId, String taskId, String status, String message) {
        send(userId, "status", Map.of(
                "taskId", taskId,
                "status", status,
                "message", message
        ));
    }

    @Override
    public void sendProgress(String userId, String taskId, long transferredBytes, long totalBytes, int transferredChunks, int totalChunks) {
        send(userId, "progress", Map.of(
                "taskId", taskId,
                "transferredBytes", transferredBytes,
                "totalBytes", totalBytes,
                "transferredChunks", transferredChunks,
                "totalChunks", totalChunks
        ));
    }

    @Override
    public void sendComplete(String userId, String taskId, String message) {
        cacheCompleteEvent(userId, taskId, message);
        send(userId, "complete", Map.of(
                "taskId", taskId,
                "message", message
        ));
    }

    @Override
    public void sendError(String userId, String taskId, String code, String message) {
        send(userId, "error", Map.of(
                "taskId", taskId,
                "code", code,
                "message", message
        ));
    }

    private void send(String userId, String eventName, Map<String, Object> payload) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null || userEmitters.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : userEmitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(payload));
            } catch (Exception e) {
                removeEmitter(userId, emitter);
            }
        }
    }

    private void scheduleHeartbeat(String userId, SseEmitter emitter) {
        ScheduledFuture<?> future = heartbeatExecutor.scheduleAtFixedRate(() -> {
            try {
                emitter.send(SseEmitter.event().comment("heartbeat"));
            } catch (Exception e) {
                log.debug("SSE heartbeat failed, removing emitter: userId={}", userId, e);
                removeEmitter(userId, emitter);
            }
        }, HEARTBEAT_INTERVAL.toSeconds(), HEARTBEAT_INTERVAL.toSeconds(), TimeUnit.SECONDS);
        heartbeatTasks.put(emitter, future);
    }

    private void replayCachedCompleteEvents(String userId, SseEmitter emitter) {
        Map<String, CachedCompleteEvent> cachedEvents = completeEventCache.get(userId);
        if (cachedEvents == null || cachedEvents.isEmpty()) {
            return;
        }
        List<String> expiredTaskIds = new ArrayList<>();
        for (Map.Entry<String, CachedCompleteEvent> entry : cachedEvents.entrySet()) {
            CachedCompleteEvent cachedEvent = entry.getValue();
            if (cachedEvent.isExpired()) {
                expiredTaskIds.add(entry.getKey());
                continue;
            }
            try {
                emitter.send(SseEmitter.event().name("complete").data(cachedEvent.payload()));
            } catch (Exception e) {
                removeEmitter(userId, emitter);
                return;
            }
        }
        expiredTaskIds.forEach(cachedEvents::remove);
        if (cachedEvents.isEmpty()) {
            completeEventCache.remove(userId);
        }
    }

    private void cacheCompleteEvent(String userId, String taskId, String message) {
        completeEventCache.computeIfAbsent(userId, key -> new ConcurrentHashMap<>())
                .put(taskId, new CachedCompleteEvent(Map.of(
                        "taskId", taskId,
                        "message", message
                ), LocalDateTime.now()));
        cleanupExpiredCompleteEvents(userId);
    }

    private void cleanupExpiredCompleteEvents(String userId) {
        Map<String, CachedCompleteEvent> cachedEvents = completeEventCache.get(userId);
        if (cachedEvents == null || cachedEvents.isEmpty()) {
            return;
        }
        cachedEvents.entrySet().removeIf(entry -> entry.getValue().isExpired());
        if (cachedEvents.isEmpty()) {
            completeEventCache.remove(userId);
        }
    }

    private void removeEmitter(String userId, SseEmitter emitter) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null) {
            return;
        }
        userEmitters.remove(emitter);
        ScheduledFuture<?> future = heartbeatTasks.remove(emitter);
        if (future != null) {
            future.cancel(true);
        }
        if (userEmitters.isEmpty()) {
            emitters.remove(userId);
        }
    }

    @PreDestroy
    public void shutdown() {
        heartbeatTasks.values().forEach(future -> future.cancel(true));
        heartbeatExecutor.shutdownNow();
    }

    private record CachedCompleteEvent(Map<String, Object> payload, LocalDateTime createdAt) {
        private boolean isExpired() {
            return createdAt.plus(COMPLETE_EVENT_TTL).isBefore(LocalDateTime.now());
        }
    }
}
