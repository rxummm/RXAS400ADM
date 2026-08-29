package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.util.BpcsRowUtil;
import com.rxas400adm.as400.MockBpcsData;
import com.rxas400adm.as400.dto.BpcsInvoiceQueryDTO;
import com.rxas400adm.as400.service.BpcsOrderServiceImpl.SysConfigServiceHolder;
import com.rxas400adm.as400.sql.SqlStatementRegistry;
import com.rxas400adm.as400.vo.BpcsInvoiceVO;
import com.rxas400adm.common.config.ProfileResolver;
import com.rxas400adm.common.response.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 【AS400 业务增强·P2】发票轨迹实现。
 * active tab: BBH/BBL（在制发票）
 * history tab: SIH/SIL（已开票历史）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BpcsInvoiceServiceImpl implements IBpcsInvoiceService {

    private final AS400ClientProvider clientProvider;
    private final ProfileResolver profileResolver;
    private final SqlStatementRegistry statements;
    private final SysConfigServiceHolder configHolder;

    @Override
    public PageResult<BpcsInvoiceVO> search(BpcsInvoiceQueryDTO query) {
        boolean isHistory = "history".equalsIgnoreCase(query.getTab());

        if (profileResolver.isMockMode()) {
            return mockSearch(query, isHistory);
        }

        String lib = configHolder.get(BpcsOrderServiceImpl.KEY_LIBRARY, "BPCSF");
        String stmtId = isHistory ? "bpcs.invoice.history" : "bpcs.invoice.active";
        String sql = statements.get(stmtId).replace("{lib}", lib);
        String ornoFilter = query.getOrno() != null ? query.getOrno() : "%";
        List<Map<String, Object>> rows = clientProvider.current()
                .queryListCheckedBounded(sql, 200,
                        query.getCono() != null ? query.getCono() : "",
                        ornoFilter);
        List<BpcsInvoiceVO> all = aggregate(rows);
        return new PageResult<>(all.size(), all);
    }

    private PageResult<BpcsInvoiceVO> mockSearch(BpcsInvoiceQueryDTO query, boolean isHistory) {
        List<Map<String, Object>> all = isHistory
                ? MockBpcsData.historicalInvoices()
                : MockBpcsData.activeInvoices();
        List<Map<String, Object>> matched = new ArrayList<>();
        for (Map<String, Object> row : all) {
            if (query.getCono() != null && !query.getCono().isBlank()
                    && !BpcsRowUtil.strEq(row, "CONO", query.getCono())) continue;
            if (query.getOrno() != null && !query.getOrno().isBlank()
                    && !BpcsRowUtil.strContains(row, "ORNO", query.getOrno())) continue;
            matched.add(row);
        }
        List<BpcsInvoiceVO> aggregated = aggregate(matched);
        long total = aggregated.size();
        int offset = (query.getCurrent() - 1) * query.getSize();
        List<BpcsInvoiceVO> paged = aggregated.stream()
                .skip(offset).limit(query.getSize()).collect(Collectors.toList());
        return new PageResult<>(total, paged);
    }

    /** 按 INVNO 聚合：汇总行 → 发票级 VO + 明细行 */
    private List<BpcsInvoiceVO> aggregate(List<Map<String, Object>> rows) {
        Map<String, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String invNo = BpcsRowUtil.pickStr(row, "INVNO");
            grouped.computeIfAbsent(invNo, k -> new ArrayList<>()).add(row);
        }

        return grouped.entrySet().stream().map(entry -> {
            List<Map<String, Object>> invRows = entry.getValue();
            Map<String, Object> first = invRows.get(0);
            String status = BpcsRowUtil.pickStr(first, "STATUS");
            String statusKey = switch (status != null ? status : "") {
                case "posted" -> "bpcs.invoiceStatus.posted";
                case "cancelled" -> "bpcs.invoiceStatus.cancelled";
                default -> "bpcs.invoiceStatus.active";
            };

            List<BpcsInvoiceVO.InvoiceLineVO> lines = invRows.stream()
                    .map(r -> new BpcsInvoiceVO.InvoiceLineVO(
                            BpcsRowUtil.pickStr(r, "LNO"), BpcsRowUtil.pickStr(r, "ITEM"),
                            BpcsRowUtil.pickStr(r, "ITDSC"), BpcsRowUtil.intOrNull(r, "QTY"),
                            BpcsRowUtil.decOrNull(r, "UPRICE"), BpcsRowUtil.decOrNull(r, "LAMT")))
                    .collect(Collectors.toList());

            return new BpcsInvoiceVO(
                    entry.getKey(),
                    BpcsRowUtil.pickStr(first, "ORNO"),
                    BpcsRowUtil.pickStr(first, "CUST"),
                    BpcsRowUtil.pickStr(first, "CUNAME"),
                    BpcsRowUtil.dateStr(first, "INVDATE"),
                    status,
                    statusKey,
                    BpcsRowUtil.decOrNull(first, "TOTAL"),
                    BpcsRowUtil.decOrNull(first, "TAX"),
                    BpcsRowUtil.intOrNull(first, "LINECT"),
                    lines);
        }).collect(Collectors.toList());
    }


}