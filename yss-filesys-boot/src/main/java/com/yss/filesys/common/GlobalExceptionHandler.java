package com.yss.filesys.common;

import com.yss.cloud.dto.response.SingleResult;
import com.yss.filesys.domain.model.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<SingleResult<Void>> handleBiz(BizException ex) {
        return ResponseEntity.badRequest().body(SingleResult.buildFailure(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<SingleResult<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().isEmpty()
                ? "请求参数不合法"
                : ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        return ResponseEntity.badRequest().body(SingleResult.buildFailure(message));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<SingleResult<Void>> handleMaxUpload(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(413)
                .body(SingleResult.buildFailure("上传文件过大，请拆分为更小的分片后重试"));
    }

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleAsyncRequestNotUsable(AsyncRequestNotUsableException ex) {
        log.debug("Ignored async request error: {}", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<SingleResult<Void>> handleUnknown(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(500).body(SingleResult.buildFailure("系统异常"));
    }
}
