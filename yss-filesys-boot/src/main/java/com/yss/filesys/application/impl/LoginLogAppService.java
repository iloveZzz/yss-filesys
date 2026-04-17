package com.yss.filesys.application.impl;

import com.yss.filesys.application.dto.LoginLogDTO;
import com.yss.filesys.application.dto.PageDTO;
import com.yss.filesys.application.port.LoginLogUseCase;
import com.yss.filesys.application.query.LoginLogPageQuery;
import com.yss.filesys.domain.gateway.LoginLogGateway;
import com.yss.filesys.domain.model.LoginLogRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LoginLogAppService implements LoginLogUseCase {

    private final LoginLogGateway loginLogGateway;

    @Override
    public void recordSuccess(String userId, String username, String loginIp, String loginAddress, String browser, String os) {
        saveLoginLog(userId, username, loginIp, loginAddress, browser, os, 0, "登录成功");
    }

    @Override
    public void recordFailure(String username, String loginIp, String loginAddress, String browser, String os, String msg) {
        saveLoginLog(null, username, loginIp, loginAddress, browser, os, 1, msg);
    }

    @Override
    public PageDTO<LoginLogDTO> page(LoginLogPageQuery query) {
        PageDTO<LoginLogRecord> page = loginLogGateway.page(
                query.getUserId(),
                query.getKeyword(),
                query.getStatus(),
                query.getPageNo(),
                query.getPageSize());
        return PageDTO.<LoginLogDTO>builder()
                .total(page.getTotal())
                .pageNo(page.getPageNo())
                .pageSize(page.getPageSize())
                .records(page.getRecords().stream().map(this::toDTO).toList())
                .build();
    }

    private void saveLoginLog(String userId, String username, String loginIp, String loginAddress, String browser,
                              String os, Integer status, String msg) {
        LoginLogRecord record = LoginLogRecord.builder()
                .userId(userId)
                .username(username)
                .loginIp(loginIp)
                .loginAddress(loginAddress)
                .browser(browser)
                .os(os)
                .status(status)
                .msg(msg)
                .loginTime(LocalDateTime.now())
                .build();
        loginLogGateway.save(record);
    }

    private LoginLogDTO toDTO(LoginLogRecord record) {
        return LoginLogDTO.builder()
                .id(record.getId())
                .userId(record.getUserId())
                .username(record.getUsername())
                .loginIp(record.getLoginIp())
                .loginAddress(record.getLoginAddress())
                .browser(record.getBrowser())
                .os(record.getOs())
                .status(record.getStatus())
                .msg(record.getMsg())
                .loginTime(record.getLoginTime())
                .build();
    }
}
