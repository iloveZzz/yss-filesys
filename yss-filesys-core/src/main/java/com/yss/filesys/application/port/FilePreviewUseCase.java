package com.yss.filesys.application.port;

import com.yss.filesys.application.dto.FilePreviewDTO;

public interface FilePreviewUseCase {

    String issueToken(String fileId);

    FilePreviewDTO preview(String fileId, String token);

    String issueArchiveToken(String archiveFileId, String innerPath);

    FilePreviewDTO previewArchive(String archiveFileId, String innerPath, String token);
}
