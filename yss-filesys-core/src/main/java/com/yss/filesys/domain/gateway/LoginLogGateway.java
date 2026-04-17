package com.yss.filesys.domain.gateway;

import com.yss.filesys.application.dto.PageDTO;
import com.yss.filesys.domain.model.LoginLogRecord;

public interface LoginLogGateway {

    void save(LoginLogRecord record);

    PageDTO<LoginLogRecord> page(String userId, String keyword, Integer status, long pageNo, long pageSize);
}
