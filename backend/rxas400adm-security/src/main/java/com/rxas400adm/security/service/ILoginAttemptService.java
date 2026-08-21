package com.rxas400adm.security.service;

import com.rxas400adm.security.entity.LoginAttempt;

import java.util.List;
import java.util.Map;

public interface ILoginAttemptService {

    void checkUsernameLock(String username, Long serverId);

    void registerFailure(String username, Long serverId, String ip);

    void clearFailure(String username, Long serverId);

    List<LoginAttempt> listAttempts(Long serverId);

    List<Map<String, Object>> aggregateByIp();

    void checkIpRate(String ip);
}