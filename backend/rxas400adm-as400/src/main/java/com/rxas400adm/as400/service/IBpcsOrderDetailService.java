package com.rxas400adm.as400.service;

import com.rxas400adm.as400.vo.BpcsOrderLineDetailVO;
import com.rxas400adm.as400.vo.BpcsOrderTimelineEventVO;

import java.util.List;

/**
 * 订单详情增强服务（⑰）。
 * 只读查询，数据源 BPCS ECL/ESH/EIN/ECH。
 */
public interface IBpcsOrderDetailService {

    /** 获取订单行增强详情（含发运和发票信息） */
    List<BpcsOrderLineDetailVO> getLineDetails(String cono, String orno);

    /** 获取订单历史事件时间线 */
    List<BpcsOrderTimelineEventVO> getTimeline(String cono, String orno);
}
