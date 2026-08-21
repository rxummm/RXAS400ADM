package com.rxas400adm.security.service;

import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.security.dto.IpRuleCreateDTO;
import com.rxas400adm.security.dto.IpRuleUpdateDTO;
import com.rxas400adm.security.entity.IpRule;

public interface IIpRuleService {

    void checkIp(String ip);

    PageResult<IpRule> page(int current, int size, String type, String keyword);

    IpRule create(IpRuleCreateDTO dto, String username);

    IpRule update(Long id, IpRuleUpdateDTO dto);

    void delete(Long id);
}