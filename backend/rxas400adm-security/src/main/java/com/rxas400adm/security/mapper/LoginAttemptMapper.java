package com.rxas400adm.security.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rxas400adm.security.entity.LoginAttempt;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * SQL 已迁至 resources/mapper/LoginAttemptMapper.xml
 */
public interface LoginAttemptMapper extends BaseMapper<LoginAttempt> {

    /** 原子累计失败次数（避免并发读改写） */
    int incrementFailure(@Param("username") String username,
                         @Param("serverId") Long serverId,
                         @Param("ip") String ip);

    /** 按 IP 聚合统计（暴力破解溯源：同 IP 多账号穷举） */
    List<Map<String, Object>> aggregateByIp();
}