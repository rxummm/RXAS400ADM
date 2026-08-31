package com.rxas400adm.as400.service;

import com.rxas400adm.as400.vo.BpcsAlertRuleVO;

import java.util.List;

/**
 * ㉜ 预警规则引擎接口。
 */
public interface IBpcsAlertEngineService {

    /** 查询库存预警 */
    List<BpcsAlertRuleVO> getAlertRules(String cono, int limit);
}
