package com.yss.filesys.service.impl;

import com.yss.filesys.service.TransferSseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class TransferSseServiceImpl implements TransferSseService {

    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    @Override
    public SseEmitter subscribe(String userId) {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.computeIfAbsent(userId, key -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError(throwable -> removeEmitter(userId, emitter));
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
            } catch (IOException e) {
                removeEmitter(userId, emitter);
            }
        }
    }

    private void removeEmitter(String userId, SseEmitter emitter) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null) {
            return;
        }
        userEmitters.remove(emitter);
        if (userEmitters.isEmpty()) {
            emitters.remove(userId);
        }
    }
}
