package com.yss.filesys.application.port;

import com.yss.filesys.application.dto.LoginLogDTO;
import com.yss.filesys.application.dto.PageDTO;
import com.yss.filesys.application.query.LoginLogPageQuery;

public interface LoginLogUseCase {

    void recordSuccess(String userId, String username, String loginIp, String loginAddress, String browser, String os);

    void recordFailure(String username, String loginIp, String loginAddress, String browser, String os, String msg);

    PageDTO<LoginLogDTO> page(LoginLogPageQuery query);
}
