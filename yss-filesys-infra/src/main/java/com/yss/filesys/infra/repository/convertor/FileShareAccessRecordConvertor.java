package com.yss.filesys.infra.repository.convertor;

import com.yss.filesys.domain.model.FileShareAccessRecord;
import com.yss.filesys.infra.repository.entity.FileShareAccessRecordPO;

public final class FileShareAccessRecordConvertor {

    private FileShareAccessRecordConvertor() {
    }

    public static FileShareAccessRecord toDomain(FileShareAccessRecordPO po) {
        return FileShareAccessRecord.builder()
                .id(po.getId())
                .shareId(po.getShareId())
                .accessIp(po.getAccessIp())
                .accessAddress(po.getAccessAddress())
                .browser(po.getBrowser())
                .os(po.getOs())
                .accessTime(po.getAccessTime())
                .build();
    }

    public static FileShareAccessRecordPO toPO(FileShareAccessRecord domain) {
        FileShareAccessRecordPO po = new FileShareAccessRecordPO();
        po.setId(domain.getId());
        po.setShareId(domain.getShareId());
        po.setAccessIp(domain.getAccessIp());
        po.setAccessAddress(domain.getAccessAddress());
        po.setBrowser(domain.getBrowser());
        po.setOs(domain.getOs());
        po.setAccessTime(domain.getAccessTime());
        return po;
    }
}
