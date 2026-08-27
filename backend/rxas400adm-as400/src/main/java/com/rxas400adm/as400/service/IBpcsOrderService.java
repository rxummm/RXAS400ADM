package com.rxas400adm.as400.service;

import com.rxas400adm.as400.dto.BpcsOrderQueryDTO;
import com.rxas400adm.as400.vo.BpcsOrderHeaderVO;
import com.rxas400adm.as400.vo.BpcsOrderLineVO;

import java.util.List;

/**
 * 【AS400 业务增强·P1】BPCS 客户订单查询（只读）。
 *
 * <p>数据源：BPCS 客户订单文件 ECH（头）/ ECL（行），只读访问，
 * 全部查询经 SqlReadOnlyValidator 白名单 + 参数占位符 + 行数上限三重护栏。
 */
public interface IBpcsOrderService {

    /** 订单头 + 进程时间轴（时间轴已按环境事实裁剪：CHSTS3/4 恒 0 不输出节点） */
    BpcsOrderHeaderVO getHeader(BpcsOrderQueryDTO query);

    /** 订单行全集（单订单行数量级为几十，一次拉取 ≤500 行，前端不再分页） */
    List<BpcsOrderLineVO> getLines(BpcsOrderQueryDTO query);
}
