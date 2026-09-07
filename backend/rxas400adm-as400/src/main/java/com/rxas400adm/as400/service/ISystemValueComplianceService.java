package com.rxas400adm.as400.service;

import com.rxas400adm.as400.vo.SystemValueComplianceVO;

import java.util.List;

/**
 * A8 系统值合规检查服务接口
 */
public interface ISystemValueComplianceService {

    /** 查询合规检查列表 */
    List<SystemValueComplianceVO> listByServer(Long serverId);

    /** 获取合规统计 */
    SystemValueComplianceVO getLatestByServer(Long serverId);
}
