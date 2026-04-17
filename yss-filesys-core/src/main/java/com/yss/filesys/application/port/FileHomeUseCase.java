package com.yss.filesys.application.port;

import com.yss.filesys.application.dto.FileHomeDTO;

public interface FileHomeUseCase {

    FileHomeDTO getHome(String userId, Integer unit, Integer dateType);
}
