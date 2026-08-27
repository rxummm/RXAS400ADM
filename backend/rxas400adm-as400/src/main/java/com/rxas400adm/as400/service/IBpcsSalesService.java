package com.rxas400adm.as400.service;

import com.rxas400adm.as400.dto.BpcsSalesQueryDTO;
import com.rxas400adm.as400.vo.BpcsSalesTrendVO;

/**
 * 【AS400 业务增强·P2】销售趋势查询（只读）。
 */
public interface IBpcsSalesService {
    /** 按时间范围返回月度销售趋势 */
    BpcsSalesTrendVO getTrend(BpcsSalesQueryDTO query);
}
