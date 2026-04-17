package com.yss.filesys.infra.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yss.filesys.infra.repository.entity.FileShareAccessRecordPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FileShareAccessRecordMapper extends BaseMapper<FileShareAccessRecordPO> {
}
