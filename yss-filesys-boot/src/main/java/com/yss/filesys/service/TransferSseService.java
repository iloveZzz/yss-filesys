package com.yss.filesys.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface TransferSseService {

    SseEmitter subscribe(String userId);

    void sendStatus(String userId, String taskId, String status, String message);

    void sendProgress(String userId, String taskId, long transferredBytes, long totalBytes, int transferredChunks, int totalChunks);

    void sendComplete(String userId, String taskId, String message);

    void sendError(String userId, String taskId, String code, String message);
}
