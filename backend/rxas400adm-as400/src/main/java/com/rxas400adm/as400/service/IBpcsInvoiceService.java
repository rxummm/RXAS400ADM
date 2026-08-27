package com.rxas400adm.as400.service;

import com.rxas400adm.as400.dto.BpcsInvoiceQueryDTO;
import com.rxas400adm.as400.vo.BpcsInvoiceVO;
import com.rxas400adm.common.response.PageResult;

/**
 * 【AS400 业务增强·P2】发票轨迹查询（只读）。
 */
public interface IBpcsInvoiceService {
    /** 按 tab(active/history) + 条件搜索发票，支持分页 */
    PageResult<BpcsInvoiceVO> search(BpcsInvoiceQueryDTO query);
}
