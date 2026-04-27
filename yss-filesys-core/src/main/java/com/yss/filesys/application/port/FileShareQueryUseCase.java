package com.yss.filesys.application.port;

import com.yss.filesys.application.dto.FileDownloadDTO;
import com.yss.filesys.application.dto.FileShareDTO;
import com.yss.filesys.application.dto.PageDTO;
import com.yss.filesys.application.dto.FileRecordDTO;
import com.yss.filesys.application.dto.FileShareThinDTO;

import java.util.List;

public interface FileShareQueryUseCase {

    List<FileShareDTO> listByUserId(String userId);

    PageDTO<FileShareDTO> pageByUserId(String userId, long pageIndex, long pageSize);

    FileShareDTO getById(String shareId);

    FileShareThinDTO getShareInfo(String shareId);

    List<FileRecordDTO> listShareFiles(String shareId, String shareCode);

    boolean verifyShareCode(String shareId, String shareCode);

    FileDownloadDTO downloadShareFile(String shareId, String fileId, String shareCode);
}
