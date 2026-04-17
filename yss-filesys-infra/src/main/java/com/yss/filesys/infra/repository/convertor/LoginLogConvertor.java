package com.yss.filesys.infra.repository.convertor;

import com.yss.filesys.domain.model.LoginLogRecord;
import com.yss.filesys.infra.repository.entity.LoginLogPO;

public final class LoginLogConvertor {

    private LoginLogConvertor() {
    }

    public static LoginLogRecord toDomain(LoginLogPO po) {
        return LoginLogRecord.builder()
                .id(po.getId())
                .userId(po.getUserId())
                .username(po.getUsername())
                .loginIp(po.getLoginIp())
                .loginAddress(po.getLoginAddress())
                .browser(po.getBrowser())
                .os(po.getOs())
                .status(po.getStatus())
                .msg(po.getMsg())
                .loginTime(po.getLoginTime())
                .build();
    }

    public static LoginLogPO toPO(LoginLogRecord record) {
        LoginLogPO po = new LoginLogPO();
        po.setId(record.getId());
        po.setUserId(record.getUserId());
        po.setUsername(record.getUsername());
        po.setLoginIp(record.getLoginIp());
        po.setLoginAddress(record.getLoginAddress());
        po.setBrowser(record.getBrowser());
        po.setOs(record.getOs());
        po.setStatus(record.getStatus());
        po.setMsg(record.getMsg());
        po.setLoginTime(record.getLoginTime());
        return po;
    }
}
