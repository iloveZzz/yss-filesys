package com.yss.filesys.infra.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yss.filesys.infra.repository.entity.FileShareItemPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FileShareItemMapper extends BaseMapper<FileShareItemPO> {
}
