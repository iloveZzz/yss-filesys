package com.yss.filesys.feignsdk.exception;

/**
 * yss-filesys Feign SDK 调用异常。
 */
public class YssFilesysFeignSdkException extends RuntimeException {

    public YssFilesysFeignSdkException(String message) {
        super(message);
    }

    public YssFilesysFeignSdkException(String message, Throwable cause) {
        super(message, cause);
    }
}
