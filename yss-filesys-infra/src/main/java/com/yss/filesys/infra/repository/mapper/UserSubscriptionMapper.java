package com.yss.filesys.infra.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yss.filesys.infra.repository.entity.UserSubscriptionPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserSubscriptionMapper extends BaseMapper<UserSubscriptionPO> {
}
