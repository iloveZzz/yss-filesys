package com.yss.filesys.domain.model;

public enum TransferTaskStatus {
    initialized,
    checking,
    uploading,
    merging,
    downloading,
    failed,
    paused,
    completed,
    canceled
}
