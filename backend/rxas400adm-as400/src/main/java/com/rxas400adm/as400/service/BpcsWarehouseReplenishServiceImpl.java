package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.MockBpcsData;
import com.rxas400adm.as400.util.BpcsRowUtil;
import com.rxas400adm.as400.dto.BpcsWarehouseReplenishQueryDTO;
import com.rxas400adm.as400.service.BpcsOrderServiceImpl.SysConfigServiceHolder;
import com.rxas400adm.as400.sql.SqlStatementRegistry;
import com.rxas400adm.as400.vo.BpcsWarehouseReplenishVO;
import com.rxas400adm.common.config.ProfileResolver;
import com.rxas400adm.common.response.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 多仓库联合补货实现。
 * 数据源 IWI（仓库库存）+ IIM（物料主档）+ ITL（库存变动）+ HPO（在途 PO）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BpcsWarehouseReplenishServiceImpl implements IBpcsWarehouseReplenishService {

    private static final String KEY_LIBRARY = "bpcs.library";
    private static final String DEFAULT_LIBRARY = "BPCSF";

    private final AS400ClientProvider clientProvider;
    private final ProfileResolver profileResolver;
    private final SqlStatementRegistry statements;
    private final SysConfigServiceHolder configHolder;

    @Override
    public PageResult<BpcsWarehouseReplenishVO> search(BpcsWarehouseReplenishQueryDTO query) {
        if (profileResolver.isMockMode()) {
            return mockSearch(query);
        }
        String lib = configHolder.get(KEY_LIBRARY, DEFAULT_LIBRARY);

        // 1) 查询各仓库库存明细
        String sqlWh = statements.get("bpcs.whReplenish.list").replace("{lib}", lib);
        String itemFilter = query.getItem() != null ? "%" + query.getItem() + "%" : "%";
        String itdscFilter = query.getItdsc() != null ? "%" + query.getItdsc() + "%" : "%";
        List<Map<String, Object>> whRows = clientProvider.current()
                .queryListCheckedBounded(sqlWh, 500,
                        query.getCono() != null ? query.getCono() : "",
                        itemFilter, itdscFilter);

        // 2) 查询物料级汇总（总库存、安全库存、平均日消耗）
        String sqlSummary = statements.get("bpcs.whReplenish.summary").replace("{lib}", lib);
        String ninetyDaysAgo = LocalDate.now().minusDays(90).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        List<Map<String, Object>> summaryRows = clientProvider.current()
                .queryListCheckedBounded(sqlSummary, 500,
                        ninetyDaysAgo,
                        query.getCono() != null ? query.getCono() : "",
                        itemFilter, itdscFilter);

        // 3) 按物料聚合仓库明细
        Map<String, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
        for (Map<String, Object> row : whRows) {
            String item = BpcsRowUtil.pickStr(row, "ITEM");
            grouped.computeIfAbsent(item, k -> new ArrayList<>()).add(row);
        }

        // 4) 构建 VO
        List<BpcsWarehouseReplenishVO> all = new ArrayList<>();
        // 用 summary 做物料级汇总
        Map<String, Map<String, Object>> summaryMap = new LinkedHashMap<>();
        for (Map<String, Object> s : summaryRows) {
            summaryMap.put(BpcsRowUtil.pickStr(s, "ITEM"), s);
        }

        for (Map.Entry<String, List<Map<String, Object>>> entry : grouped.entrySet()) {
            String item = entry.getKey();
            List<Map<String, Object>> rows = entry.getValue();
            Map<String, Object> first = rows.get(0);

            Map<String, Object> summary = summaryMap.get(item);
            BigDecimal totalQty = summary != null ? BpcsRowUtil.decOrNull(summary, "TOTAL_QTY") : BigDecimal.ZERO;
            BigDecimal safety = summary != null ? BpcsRowUtil.decOrNull(summary, "SAFETY") : BigDecimal.ZERO;
            BigDecimal avgDemand = summary != null ? BpcsRowUtil.decOrNull(summary, "AVG_DEMAND") : null;

            // 缺口 = 安全库存 - 总库存（负数表示超额）
            BigDecimal shortage = safety.subtract(totalQty != null ? totalQty : BigDecimal.ZERO);

            // 建议补货量 = max(缺口, 0) + 7天安全缓冲
            BigDecimal suggestQty = BigDecimal.ZERO;
            if (shortage.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal buffer = avgDemand != null ? avgDemand.multiply(BigDecimal.valueOf(7)).setScale(0, RoundingMode.CEILING) : BigDecimal.ZERO;
                suggestQty = shortage.add(buffer);
            }

            List<BpcsWarehouseReplenishVO.WarehouseStockVO> whVos = rows.stream().map(r -> {
                BigDecimal qtyOh = BpcsRowUtil.decOrNull(r, "QTYOH");
                BigDecimal qtyAvc = totalQty != null && totalQty.compareTo(BigDecimal.ZERO) > 0
                        ? (qtyOh != null ? qtyOh : BigDecimal.ZERO).divide(totalQty, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
                return new BpcsWarehouseReplenishVO.WarehouseStockVO(
                        BpcsRowUtil.pickStr(r, "WH"),
                        qtyOh,
                        BpcsRowUtil.decOrNull(r, "QTYALC"),
                        BpcsRowUtil.decOrNull(r, "QTYAVL"),
                        BpcsRowUtil.decOrNull(r, "QTYONORD"),
                        BpcsRowUtil.dateStr(r, "LAST_TXN_DATE"),
                        qtyAvc
                );
            }).collect(Collectors.toList());

            all.add(new BpcsWarehouseReplenishVO(
                    item,
                    BpcsRowUtil.pickStr(first, "ITDSC"),
                    totalQty,
                    safety,
                    shortage,
                    avgDemand,
                    suggestQty,
                    whVos
            ));
        }

        // 过滤：仅显示低于安全库存的物料
        if (query.isBelowSafetyOnly()) {
            all = all.stream()
                    .filter(v -> v.shortage() != null && v.shortage().compareTo(BigDecimal.ZERO) > 0)
                    .collect(Collectors.toList());
        }

        // 分页
        long total = all.size();
        int offset = (query.getCurrent() - 1) * query.getSize();
        List<BpcsWarehouseReplenishVO> paged = all.stream()
                .skip(offset).limit(query.getSize()).collect(Collectors.toList());
        return new PageResult<>(total, paged);
    }

    private PageResult<BpcsWarehouseReplenishVO> mockSearch(BpcsWarehouseReplenishQueryDTO query) {
        List<Map<String, Object>> rawRows = MockBpcsData.warehouseReplenishData();
        // 按 ITEM 聚合
        Map<String, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
        for (Map<String, Object> row : rawRows) {
            String item = BpcsRowUtil.pickStr(row, "ITEM");
            grouped.computeIfAbsent(item, k -> new ArrayList<>()).add(row);
        }
        List<BpcsWarehouseReplenishVO> all = new ArrayList<>();
        for (Map.Entry<String, List<Map<String, Object>>> entry : grouped.entrySet()) {
            List<Map<String, Object>> rows = entry.getValue();
            Map<String, Object> first = rows.get(0);
            BigDecimal totalQty = rows.stream()
                    .map(r -> BpcsRowUtil.decOrNull(r, "QTYOH"))
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal safety = BpcsRowUtil.decOrNull(first, "SAFETY");
            BigDecimal shortage = safety != null ? safety.subtract(totalQty) : BigDecimal.ZERO;
            BigDecimal avgDemand = BpcsRowUtil.decOrNull(first, "AVG_DEMAND");
            BigDecimal suggestQty = BigDecimal.ZERO;
            if (shortage.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal buffer = avgDemand != null ? avgDemand.multiply(BigDecimal.valueOf(7)).setScale(0, java.math.RoundingMode.CEILING) : BigDecimal.ZERO;
                suggestQty = shortage.add(buffer);
            }
            List<BpcsWarehouseReplenishVO.WarehouseStockVO> whVos = rows.stream().map(r -> {
                BigDecimal qtyOh = BpcsRowUtil.decOrNull(r, "QTYOH");
                BigDecimal pct = totalQty.compareTo(BigDecimal.ZERO) > 0
                        ? (qtyOh != null ? qtyOh : BigDecimal.ZERO).divide(totalQty, 4, java.math.RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(1, java.math.RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
                return new BpcsWarehouseReplenishVO.WarehouseStockVO(
                        BpcsRowUtil.pickStr(r, "WH"), qtyOh,
                        BpcsRowUtil.decOrNull(r, "QTYALC"), BpcsRowUtil.decOrNull(r, "QTYAVL"),
                        BpcsRowUtil.decOrNull(r, "QTYONORD"),
                        BpcsRowUtil.dateStr(r, "LAST_TXN_DATE"), pct);
            }).collect(Collectors.toList());
            all.add(new BpcsWarehouseReplenishVO(
                    entry.getKey(), BpcsRowUtil.pickStr(first, "ITDSC"),
                    totalQty, safety, shortage, avgDemand, suggestQty, whVos));
        }
        if (query.isBelowSafetyOnly()) {
            all = all.stream()
                    .filter(v -> v.shortage() != null && v.shortage().compareTo(BigDecimal.ZERO) > 0)
                    .collect(Collectors.toList());
        }
        long total = all.size();
        int offset = (query.getCurrent() - 1) * query.getSize();
        List<BpcsWarehouseReplenishVO> paged = all.stream()
                .skip(offset).limit(query.getSize()).collect(Collectors.toList());
        return new PageResult<>(total, paged);
    }
}
