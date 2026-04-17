package com.yss.filesys.application.command;

import lombok.Data;

@Data
public class CreateShareAccessRecordCommand {
    private String shareId;
    private String accessIp;
    private String accessAddress;
    private String browser;
    private String os;
}
