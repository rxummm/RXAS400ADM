package com.rxas400adm.as400.service;

import com.rxas400adm.as400.vo.SecurityAuditSummaryVO;
import com.rxas400adm.as400.vo.SystemHealthOverviewVO;
import com.rxas400adm.as400.vo.UserPermissionMatrixVO;

import java.util.List;

/**
 * A1/A2/A3 系统健康与安全服务接口
 */
public interface ISystemHealthService {

    /** A1: 系统健康概览（聚合多服务器指标） */
    SystemHealthOverviewVO getOverview();

    /** A2: 安全审计摘要 */
    List<SecurityAuditSummaryVO> getSecurityAuditSummary();

    /** A3: 用户权限矩阵 */
    List<UserPermissionMatrixVO> getUserPermissionMatrix();
}
