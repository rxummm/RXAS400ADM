package com.rxas400adm.as400.service;

import com.rxas400adm.as400.vo.SystemValueComplianceVO;
import com.rxas400adm.common.response.PageResult;

import java.util.List;

/**
 * A8 系统值合规检查服务接口
 */
public interface ISystemValueComplianceService {

    /** 查询合规检查列表（分页） */
    PageResult<SystemValueComplianceVO> listByServer(Long serverId, int current, int size);

    /** 查询合规检查列表（兼容旧版，不分页） */
    @Deprecated
    default List<SystemValueComplianceVO> listByServer(Long serverId) {
        return listByServer(serverId, 1, Integer.MAX_VALUE).getRecords();
    }

    /** 获取合规统计 */
    SystemValueComplianceVO getLatestByServer(Long serverId);
}
