package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.util.BpcsRowUtil;
import com.rxas400adm.as400.MockBpcsData;
import com.rxas400adm.as400.dto.BpcsInventoryQueryDTO;
import com.rxas400adm.as400.service.BpcsOrderServiceImpl.SysConfigServiceHolder;
import com.rxas400adm.as400.sql.SqlStatementRegistry;
import com.rxas400adm.as400.vo.BpcsInventoryVO;
import com.rxas400adm.common.config.ProfileResolver;
import com.rxas400adm.common.response.PageResult;
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
 * 【AS400 业务增强·P2】库存可用量查询实现。
 * 数据源：IIM（物料主档）+ IWI（仓库×物料库存），只读访问。
 * 可用量 = 在手(IOHB) - 已分配(IISSU) + 在途(IRCT) + 调整(IADJU)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BpcsInventoryServiceImpl implements IBpcsInventoryService {

    private final AS400ClientProvider clientProvider;
    private final ProfileResolver profileResolver;
    private final SqlStatementRegistry statements;
    private final SysConfigServiceHolder configHolder;

    @Override
    public PageResult<BpcsInventoryVO> search(BpcsInventoryQueryDTO query) {
        if (profileResolver.isMockMode()) {
            return mockSearch(query);
        }
        String lib = configHolder.get(BpcsOrderServiceImpl.KEY_LIBRARY, "BPCSF");
        String sql = statements.get("bpcs.inventory.search").replace("{lib}", lib);
        String itemFilter = query.getItem() != null ? "%" + query.getItem() + "%" : "%";
        List<Map<String, Object>> rows = clientProvider.current()
                .queryListCheckedBounded(sql, 500,
                        query.getCono() != null ? query.getCono() : "",
                        itemFilter,
                        query.getWh() != null ? query.getWh() : "%");
        List<BpcsInventoryVO> all = aggregate(rows);
        return new PageResult<>(all.size(), all);
    }

    private PageResult<BpcsInventoryVO> mockSearch(BpcsInventoryQueryDTO query) {
        List<Map<String, Object>> all = MockBpcsData.inventoryItems();
        List<Map<String, Object>> matched = new ArrayList<>();
        for (Map<String, Object> row : all) {
            if (query.getCono() != null && !query.getCono().isBlank()
                    && !BpcsRowUtil.strEq(row, "CONO", query.getCono())) continue;
            if (query.getItem() != null && !query.getItem().isBlank()
                    && !BpcsRowUtil.strContains(row, "ITEM", query.getItem())) continue;
            if (query.getDesc() != null && !query.getDesc().isBlank()
                    && !BpcsRowUtil.strContains(row, "ITDSC", query.getDesc())) continue;
            if (query.getWh() != null && !query.getWh().isBlank()
                    && !BpcsRowUtil.strEq(row, "WH", query.getWh())) continue;
            matched.add(row);
        }
        List<BpcsInventoryVO> aggregated = aggregate(matched);
        long total = aggregated.size();
        int offset = (query.getCurrent() - 1) * query.getSize();
        List<BpcsInventoryVO> paged = aggregated.stream()
                .skip(offset).limit(query.getSize()).collect(Collectors.toList());
        return new PageResult<>(total, paged);
    }

    /** 按 ITEM 聚合：汇总各仓库数量 → 物料级 VO + 仓库明细 */
    private List<BpcsInventoryVO> aggregate(List<Map<String, Object>> rows) {
        // 按 ITEM 分组
        Map<String, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String item = BpcsRowUtil.pickStr(row, "ITEM");
            grouped.computeIfAbsent(item, k -> new ArrayList<>()).add(row);
        }

        return grouped.entrySet().stream().map(entry -> {
            List<Map<String, Object>> whRows = entry.getValue();
            Map<String, Object> first = whRows.get(0);

            int totalOnHand = 0, totalAlloc = 0, totalOnOrder = 0;
            List<BpcsInventoryVO.WhDetailVO> whDetails = new ArrayList<>();

            for (Map<String, Object> r : whRows) {
                int oh = BpcsRowUtil.intVal(r, "IOHB");
                int alloc = BpcsRowUtil.intVal(r, "IISSU");
                int oo = BpcsRowUtil.intVal(r, "IRCT");
                int adj = BpcsRowUtil.intVal(r, "IADJU");
                int avail = oh - alloc + oo + adj;
                totalOnHand += oh;
                totalAlloc += alloc;
                totalOnOrder += oo;
                whDetails.add(new BpcsInventoryVO.WhDetailVO(
                        BpcsRowUtil.pickStr(r, "WH"), oh, alloc, oo, avail, BpcsRowUtil.pickStr(r, "LOC")));
            }

            return new BpcsInventoryVO(
                    entry.getKey(),
                    BpcsRowUtil.pickStr(first, "ITDSC"),
                    BpcsRowUtil.pickStr(first, "UOM"),
                    totalOnHand, totalAlloc, totalOnOrder,
                    totalOnHand - totalAlloc + totalOnOrder,
                    BpcsRowUtil.decOrNull(first, "ICOST"),
                    whDetails);
        }).collect(Collectors.toList());
    }

}
