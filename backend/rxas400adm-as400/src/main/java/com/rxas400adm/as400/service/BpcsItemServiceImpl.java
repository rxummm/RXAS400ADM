package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.util.BpcsRowUtil;
import com.rxas400adm.as400.MockBpcsData;
import com.rxas400adm.as400.dto.BpcsItemQueryDTO;
import com.rxas400adm.as400.service.BpcsOrderServiceImpl.SysConfigServiceHolder;
import com.rxas400adm.as400.sql.SqlStatementRegistry;
import com.rxas400adm.as400.vo.BpcsItemVO;
import com.rxas400adm.common.config.ProfileResolver;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 【AS400 业务增强·P2】物料主档查询实现。
 * 多维度聚合：IIM（主档）+ IWI（库存）+ HPO（采购历史）+ SSD（销售历史）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BpcsItemServiceImpl implements IBpcsItemService {

    private final AS400ClientProvider clientProvider;
    private final ProfileResolver profileResolver;
    private final SqlStatementRegistry statements;
    private final SysConfigServiceHolder configHolder;

    @Override
    public PageResult<BpcsItemVO> search(BpcsItemQueryDTO query) {
        if (profileResolver.isMockMode()) {
            return mockSearch(query);
        }
        String lib = configHolder.get(BpcsOrderServiceImpl.KEY_LIBRARY, "BPCSF");
        String sql = statements.get("bpcs.item.search").replace("{lib}", lib);
        String itemFilter = query.getItem() != null ? "%" + query.getItem() + "%" : "%";
        List<Map<String, Object>> rows = clientProvider.current()
                .queryListCheckedBounded(sql, 100,
                        query.getCono() != null ? query.getCono() : "",
                        itemFilter);
        // 真机：只返回基础信息（不聚合库存/采购/销售），由 getDetail 补全
        List<BpcsItemVO> all = rows.stream().map(r -> toItemVOBasic(r, List.of(), List.of(), List.of()))
                .collect(Collectors.toList());
        return new PageResult<>(all.size(), all);
    }

    @Override
    public BpcsItemVO getDetail(String item) {
        if (profileResolver.isMockMode()) {
            return mockDetail(item);
        }
        // 真机：分别查询各维度数据并聚合
        String lib = configHolder.get(BpcsOrderServiceImpl.KEY_LIBRARY, "BPCSF");
        // 基础信息
        String sqlBasic = statements.get("bpcs.item.basic").replace("{lib}", lib);
        List<Map<String, Object>> basicRows = clientProvider.current()
                .queryListCheckedBounded(sqlBasic, 1, item);
        Map<String, Object> basic = basicRows.stream().findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "物料不存在: " + item));
        // 库存
        String sqlWh = statements.get("bpcs.item.warehouse").replace("{lib}", lib);
        List<Map<String, Object>> whRows = clientProvider.current()
                .queryListCheckedBounded(sqlWh, 50, item);
        // 采购历史
        String sqlPur = statements.get("bpcs.item.purchases").replace("{lib}", lib);
        List<Map<String, Object>> purRows = clientProvider.current()
                .queryListCheckedBounded(sqlPur, 10, item);
        // 销售历史
        String sqlSal = statements.get("bpcs.item.sales").replace("{lib}", lib);
        List<Map<String, Object>> salRows = clientProvider.current()
                .queryListCheckedBounded(sqlSal, 10, item);
        return toItemVOFull(basic, whRows, purRows, salRows);
    }


    private PageResult<BpcsItemVO> mockSearch(BpcsItemQueryDTO query) {
        List<Map<String, Object>> masters = MockBpcsData.itemMasters();
        List<BpcsItemVO> all = masters.stream()
                .filter(r -> {
                    if (query.getItem() != null && !query.getItem().isBlank()
                            && !BpcsRowUtil.strContains(r, "ITEM", query.getItem())) return false;
                    if (query.getDesc() != null && !query.getDesc().isBlank()
                            && !BpcsRowUtil.strContains(r, "ITDSC", query.getDesc())) return false;
                    return true;
                })
                .map(r -> toItemVOBasic(r, List.of(), List.of(), List.of()))
                .collect(Collectors.toList());
        long total = all.size();
        int offset = (query.getCurrent() - 1) * query.getSize();
        List<BpcsItemVO> paged = all.stream()
                .skip(offset).limit(query.getSize()).collect(Collectors.toList());
        return new PageResult<>(total, paged);
    }

    private BpcsItemVO mockDetail(String item) {
        Map<String, Object> master = MockBpcsData.itemMasters().stream()
                .filter(r -> BpcsRowUtil.strEq(r, "ITEM", item))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "物料不存在: " + item));

        List<Map<String, Object>> whData = MockBpcsData.inventoryItems().stream()
                .filter(r -> BpcsRowUtil.strEq(r, "ITEM", item))
                .collect(Collectors.toList());

        List<Map<String, Object>> purData = MockBpcsData.itemPurchases().stream()
                .filter(r -> BpcsRowUtil.strEq(r, "ITEM", item))
                .collect(Collectors.toList());

        List<Map<String, Object>> salData = MockBpcsData.itemSales().stream()
                .filter(r -> BpcsRowUtil.strEq(r, "ITEM", item))
                .collect(Collectors.toList());

        return toItemVOFull(master, whData, purData, salData);
    }


    private BpcsItemVO toItemVOBasic(Map<String, Object> r,
                                      List<BpcsItemVO.WhStockVO> whs,
                                      List<BpcsItemVO.RecentPurchaseVO> purs,
                                      List<BpcsItemVO.RecentSalesVO> sals) {
        return new BpcsItemVO(
                BpcsRowUtil.pickStr(r, "ITEM"), BpcsRowUtil.pickStr(r, "ITDSC"), BpcsRowUtil.pickStr(r, "UOM"),
                BpcsRowUtil.pickStr(r, "ICAT"), BpcsRowUtil.decOrNull(r, "ICOST"), BpcsRowUtil.decOrNull(r, "ILPRT"),
                BpcsRowUtil.dblOrNull(r, "IWEIGHT"), BpcsRowUtil.intOrNull(r, "ISHLF"),
                whs, 0, 0, 0, purs, sals);
    }

    private BpcsItemVO toItemVOFull(Map<String, Object> master,
                                     List<Map<String, Object>> whRows,
                                     List<Map<String, Object>> purRows,
                                     List<Map<String, Object>> salRows) {
        // 库存聚合
        List<BpcsItemVO.WhStockVO> whs = new ArrayList<>();
        int totalOh = 0, totalAlloc = 0;
        for (Map<String, Object> r : whRows) {
            int oh = BpcsRowUtil.intVal(r, "IOHB");
            int alloc = BpcsRowUtil.intVal(r, "IISSU");
            int oo = BpcsRowUtil.intVal(r, "IRCT");
            int adj = BpcsRowUtil.intVal(r, "IADJU");
            int avail = oh - alloc + oo + adj;
            totalOh += oh;
            totalAlloc += alloc;
            whs.add(new BpcsItemVO.WhStockVO(
                    BpcsRowUtil.pickStr(r, "WH"), BpcsRowUtil.pickStr(r, "LOC"), oh, alloc, oo, avail));
        }

        // 采购历史
        List<BpcsItemVO.RecentPurchaseVO> purs = purRows.stream()
                .map(r -> new BpcsItemVO.RecentPurchaseVO(
                        BpcsRowUtil.pickStr(r, "PONO"), BpcsRowUtil.pickStr(r, "VNAME"),
                        BpcsRowUtil.dateStr(r, "ODATE"), BpcsRowUtil.intOrNull(r, "QTY"), BpcsRowUtil.decOrNull(r, "UPRICE")))
                .collect(Collectors.toList());

        // 销售历史
        List<BpcsItemVO.RecentSalesVO> sals = salRows.stream()
                .map(r -> new BpcsItemVO.RecentSalesVO(
                        BpcsRowUtil.pickStr(r, "ORNO"), BpcsRowUtil.pickStr(r, "CNAME"),
                        BpcsRowUtil.dateStr(r, "ODATE"), BpcsRowUtil.intOrNull(r, "QTY"), BpcsRowUtil.decOrNull(r, "UPRICE")))
                .collect(Collectors.toList());

        return new BpcsItemVO(
                BpcsRowUtil.pickStr(master, "ITEM"), BpcsRowUtil.pickStr(master, "ITDSC"), BpcsRowUtil.pickStr(master, "UOM"),
                BpcsRowUtil.pickStr(master, "ICAT"), BpcsRowUtil.decOrNull(master, "ICOST"), BpcsRowUtil.decOrNull(master, "ILPRT"),
                BpcsRowUtil.dblOrNull(master, "IWEIGHT"), BpcsRowUtil.intOrNull(master, "ISHLF"),
                whs, totalOh, totalAlloc, totalOh - totalAlloc, purs, sals);
    }

}