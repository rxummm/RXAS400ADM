package com.rxas400adm.security.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rxas400adm.security.entity.TokenBlacklist;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TokenBlacklistMapper extends BaseMapper<TokenBlacklist> {
}
