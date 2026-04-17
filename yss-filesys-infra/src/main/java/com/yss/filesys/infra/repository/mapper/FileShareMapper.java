package com.yss.filesys.infra.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yss.filesys.infra.repository.entity.FileSharePO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FileShareMapper extends BaseMapper<FileSharePO> {
}
