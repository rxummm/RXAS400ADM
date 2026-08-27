package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.util.BpcsRowUtil;
import com.rxas400adm.as400.MockBpcsData;
import com.rxas400adm.as400.dto.BpcsPurchaseQueryDTO;
import com.rxas400adm.as400.service.BpcsOrderServiceImpl.SysConfigServiceHolder;
import com.rxas400adm.as400.sql.SqlStatementRegistry;
import com.rxas400adm.as400.vo.BpcsPurchaseOrderVO;
import com.rxas400adm.common.config.ProfileResolver;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.common.util.BpcsDateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 【AS400 业务增强·P2】采购订单实现。
 * 数据源 HPH（头）+ HPO（行），只读访问。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BpcsPurchaseServiceImpl implements IBpcsPurchaseService {

    static final String[] PURCHASE_STATUS_KEYS = {"open", "partial", "complete", "closed"};

    private final AS400ClientProvider clientProvider;
    private final ProfileResolver profileResolver;
    private final SqlStatementRegistry statements;
    private final SysConfigServiceHolder configHolder;

    @Override
    public PageResult<BpcsPurchaseOrderVO> search(BpcsPurchaseQueryDTO query) {
        if (profileResolver.isMockMode()) {
            return mockSearch(query);
        }
        String lib = configHolder.get(BpcsOrderServiceImpl.KEY_LIBRARY, "BPCSF");
        String sql = statements.get("bpcs.purchase.search").replace("{lib}", lib);
        String ponoFilter = query.getPono() != null ? query.getPono() : "%";
        String vendorFilter = query.getVendor() != null ? "%" + query.getVendor() + "%" : "%";
        List<Map<String, Object>> rows = clientProvider.current()
                .queryListCheckedBounded(sql, 200,
                        query.getCono() != null ? query.getCono() : "",
                        ponoFilter, vendorFilter);
        List<BpcsPurchaseOrderVO> all = aggregate(rows);
        return new PageResult<>(all.size(), all);
    }

    private PageResult<BpcsPurchaseOrderVO> mockSearch(BpcsPurchaseQueryDTO query) {
        List<Map<String, Object>> all = MockBpcsData.purchaseOrders();
        List<Map<String, Object>> matched = new ArrayList<>();
        for (Map<String, Object> row : all) {
            if (query.getCono() != null && !query.getCono().isBlank()
                    && !BpcsRowUtil.strEq(row, "CONO", query.getCono())) continue;
            if (query.getPono() != null && !query.getPono().isBlank()
                    && !BpcsRowUtil.strContains(row, "PONO", query.getPono())) continue;
            if (query.getVendor() != null && !query.getVendor().isBlank()
                    && !BpcsRowUtil.strContains(row, "VNAME", query.getVendor())) continue;
            matched.add(row);
        }
        List<BpcsPurchaseOrderVO> aggregated = aggregate(matched);
        long total = aggregated.size();
        int offset = (query.getCurrent() - 1) * query.getSize();
        List<BpcsPurchaseOrderVO> paged = aggregated.stream()
                .skip(offset).limit(query.getSize()).collect(Collectors.toList());
        return new PageResult<>(total, paged);
    }

    /** 按 PONO 聚合 */
    private List<BpcsPurchaseOrderVO> aggregate(List<Map<String, Object>> rows) {
        Map<String, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String pono = BpcsRowUtil.pickStr(row, "PONO");
            grouped.computeIfAbsent(pono, k -> new ArrayList<>()).add(row);
        }

        return grouped.entrySet().stream().map(entry -> {
            List<Map<String, Object>> poRows = entry.getValue();
            Map<String, Object> first = poRows.get(0);
            int stat = BpcsRowUtil.intVal(first, "STATUS");
            String statKey = PURCHASE_STATUS_KEYS[Math.max(0, Math.min(stat, PURCHASE_STATUS_KEYS.length - 1))];

            List<BpcsPurchaseOrderVO.PurchaseLineVO> lines = poRows.stream()
                    .map(r -> new BpcsPurchaseOrderVO.PurchaseLineVO(
                            BpcsRowUtil.pickStr(r, "LNO"), BpcsRowUtil.pickStr(r, "ITEM"),
                            BpcsRowUtil.pickStr(r, "ITDSC"), BpcsRowUtil.intOrNull(r, "QTYORD"),
                            BpcsRowUtil.intOrNull(r, "QTYRCV"), BpcsRowUtil.decOrNull(r, "UPRICE"),
                            BpcsRowUtil.dateStr(r, "LREQDTE")))
                    .collect(Collectors.toList());

            return new BpcsPurchaseOrderVO(
                    BpcsRowUtil.pickStr(first, "CONO"),
                    entry.getKey(),
                    BpcsRowUtil.pickStr(first, "VENDOR"),
                    BpcsRowUtil.pickStr(first, "VNAME"),
                    BpcsRowUtil.dateStr(first, "PODATE"),
                    BpcsRowUtil.dateStr(first, "REQDTE"),
                    BpcsRowUtil.decOrNull(first, "TOTAL"),
                    BpcsRowUtil.intVal(first, "LINECT"),
                    stat,
                    "bpcs.poStatus." + statKey,
                    lines);
        }).collect(Collectors.toList());
    }

}
