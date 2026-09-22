package com.rxas400adm.olap.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.olap.entity.OlapInventorySummary;
import com.rxas400adm.olap.entity.OlapPurchaseSummary;
import com.rxas400adm.olap.entity.OlapSalesSummary;
import com.rxas400adm.olap.mapper.OlapInventorySummaryMapper;
import com.rxas400adm.olap.mapper.OlapPurchaseSummaryMapper;
import com.rxas400adm.olap.mapper.OlapSalesSummaryMapper;
import com.rxas400adm.olap.vo.OlapInventorySummaryVO;
import com.rxas400adm.olap.vo.OlapPurchaseSummaryVO;
import com.rxas400adm.olap.vo.OlapSalesSummaryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OlapAnalysisService {

    private final OlapSalesSummaryMapper salesSummaryMapper;
    private final OlapInventorySummaryMapper inventorySummaryMapper;
    private final OlapPurchaseSummaryMapper purchaseSummaryMapper;

    /**
     * 查询销售分析汇总（从预聚合表 rx_olap_sales_summary）。
     */
    public PageResult<OlapSalesSummaryVO> salesSummary(String cono, String period,
                                                        int current, int size) {
        LambdaQueryWrapper<OlapSalesSummary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(cono != null && !cono.isBlank(), OlapSalesSummary::getCono, cono)
                .eq(period != null && !period.isBlank(), OlapSalesSummary::getPeriod, period)
                .orderByDesc(OlapSalesSummary::getAnalysisDate);

        Page<OlapSalesSummary> page = salesSummaryMapper.selectPage(
                new Page<>(current, size), wrapper);

        return new PageResult<>(page.getTotal(),
                page.getRecords().stream().map(OlapSalesSummaryVO::from).toList());
    }

    /**
     * 查询库存分析汇总。
     */
    public PageResult<OlapInventorySummaryVO> inventorySummary(String cono, String warehouse,
                                                                int current, int size) {
        LambdaQueryWrapper<OlapInventorySummary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(cono != null && !cono.isBlank(), OlapInventorySummary::getCono, cono)
                .eq(warehouse != null && !warehouse.isBlank(), OlapInventorySummary::getWarehouse, warehouse)
                .orderByDesc(OlapInventorySummary::getAnalysisDate);

        Page<OlapInventorySummary> page = inventorySummaryMapper.selectPage(
                new Page<>(current, size), wrapper);

        return new PageResult<>(page.getTotal(),
                page.getRecords().stream().map(OlapInventorySummaryVO::from).toList());
    }

    /**
     * 查询采购分析汇总。
     */
    public PageResult<OlapPurchaseSummaryVO> purchaseSummary(String cono, String vendorCode,
                                                              int current, int size) {
        LambdaQueryWrapper<OlapPurchaseSummary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(cono != null && !cono.isBlank(), OlapPurchaseSummary::getCono, cono)
                .eq(vendorCode != null && !vendorCode.isBlank(), OlapPurchaseSummary::getVendorCode, vendorCode)
                .orderByDesc(OlapPurchaseSummary::getAnalysisDate);

        Page<OlapPurchaseSummary> page = purchaseSummaryMapper.selectPage(
                new Page<>(current, size), wrapper);

        return new PageResult<>(page.getTotal(),
                page.getRecords().stream().map(OlapPurchaseSummaryVO::from).toList());
    }
}
