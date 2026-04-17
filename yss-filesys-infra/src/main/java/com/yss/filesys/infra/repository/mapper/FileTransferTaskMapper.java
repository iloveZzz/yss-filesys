package com.yss.filesys.infra.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yss.filesys.infra.repository.entity.FileTransferTaskPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FileTransferTaskMapper extends BaseMapper<FileTransferTaskPO> {
}
