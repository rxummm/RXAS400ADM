package com.rxas400adm.as400.service;

import com.rxas400adm.as400.vo.BpcsAlertRuleVO;
import com.rxas400adm.common.response.PageResult;

/**
 * ㉜ 预警规则引擎接口。
 */
public interface IBpcsAlertEngineService {

    /** 查询库存预警 */
    PageResult<BpcsAlertRuleVO> getAlertRules(String cono, int current, int size);
}
