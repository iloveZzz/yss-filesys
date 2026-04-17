package com.yss.filesys.application.port;

import com.yss.filesys.application.command.CreateShareCommand;
import com.yss.filesys.application.dto.FileShareDTO;

import java.util.List;

public interface FileShareCommandUseCase {

    FileShareDTO create(CreateShareCommand command);

    void cancelByIds(List<String> shareIds);
}
