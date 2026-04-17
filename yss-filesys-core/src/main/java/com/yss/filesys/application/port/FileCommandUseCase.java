package com.yss.filesys.application.port;

import com.yss.filesys.application.command.CreateDirectoryCommand;
import com.yss.filesys.application.command.MoveToRecycleBinCommand;
import com.yss.filesys.application.command.MoveFileCommand;
import com.yss.filesys.application.command.RenameFileCommand;
import com.yss.filesys.application.dto.FileRecordDTO;

public interface FileCommandUseCase {

    FileRecordDTO createDirectory(CreateDirectoryCommand command);

    void moveToRecycleBin(MoveToRecycleBinCommand command);

    void renameFile(String fileId, RenameFileCommand command);

    void moveFile(MoveFileCommand command);
}
