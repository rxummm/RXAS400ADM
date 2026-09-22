package com.rxas400adm.as400.service;

import com.rxas400adm.as400.dto.CycleCountPlanDTO;
import com.rxas400adm.as400.dto.CycleCountResultDTO;
import com.rxas400adm.as400.vo.CycleCountPlanVO;
import com.rxas400adm.as400.vo.CycleCountResultVO;
import com.rxas400adm.common.response.PageResult;

/**
 * 循环盘点服务（㉙）。
 */
public interface ICycleCountService {

    /** 创建盘点计划 */
    CycleCountPlanVO createPlan(CycleCountPlanDTO dto, String operator);

    /** 获取盘点计划列表 */
    PageResult<CycleCountPlanVO> listPlans(String status, int current, int size);

    /** 录入盘点结果 */
    CycleCountResultVO recordResult(CycleCountResultDTO dto, String operator, int systemQty);

    /** 获取盘点结果列表 */
    PageResult<CycleCountResultVO> listResults(Long planId, int current, int size);

    /** 获取盘点汇总统计 */
    Object getSummary(String fromDate, String toDate);
}
