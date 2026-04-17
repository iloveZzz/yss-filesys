package com.yss.filesys.application.port;

import com.yss.filesys.application.command.CreateShareAccessRecordCommand;
import com.yss.filesys.application.dto.FileShareAccessRecordDTO;

import java.util.List;

public interface FileShareAccessUseCase {

    void record(CreateShareAccessRecordCommand command);

    List<FileShareAccessRecordDTO> listByShareId(String shareId);
}
