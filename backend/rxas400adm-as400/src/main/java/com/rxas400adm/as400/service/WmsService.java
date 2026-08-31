package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.dto.*;
import com.rxas400adm.as400.service.BpcsOrderServiceImpl.SysConfigServiceHolder;
import com.rxas400adm.as400.sql.SqlStatementRegistry;
import com.rxas400adm.as400.vo.*;
import com.rxas400adm.common.config.ProfileResolver;
import com.rxas400adm.common.response.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * WMS 仓库管理服务实现。
 * 数据源：WHS（仓库）+ BIN（库位）+ IBL（库位库存）+ ITH（事务日志），只读访问。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WmsService implements IWmsService {

    private final AS400ClientProvider clientProvider;
    private final ProfileResolver profileResolver;
    private final SqlStatementRegistry statements;
    private final SysConfigServiceHolder configHolder;

    @Override
    public PageResult<BpcsWarehouseVO> searchWarehouses(BpcsWarehouseQueryDTO query) {
        if (profileResolver.isMockMode()) {
            return mockWarehouses(query);
        }
        String lib = configHolder.get(BpcsOrderServiceImpl.KEY_LIBRARY, "BPCSF");
        String sql = statements.get("bpcs.wms.warehouse").replace("{lib}", lib);
        List<Map<String, Object>> rows = clientProvider.current()
                .queryListCheckedBounded(sql, query.getSize(),
                        query.getCono(),
                        like(query.getWhse()),
                        like(query.getWhname()),
                        query.getSize());
        List<BpcsWarehouseVO> list = rows.stream().map(this::toWarehouseVO).toList();
        return new PageResult<>(list.size(), list);
    }

    @Override
    public PageResult<BpcsBinVO> searchBins(BpcsBinQueryDTO query) {
        if (profileResolver.isMockMode()) {
            return mockBins(query);
        }
        String lib = configHolder.get(BpcsOrderServiceImpl.KEY_LIBRARY, "BPCSF");
        String sql = statements.get("bpcs.wms.bin").replace("{lib}", lib);
        List<Map<String, Object>> rows = clientProvider.current()
                .queryListCheckedBounded(sql, query.getSize(),
                        query.getCono(),
                        query.getWhse() != null ? query.getWhse() : "",
                        like(query.getBinno()),
                        query.getSize());
        List<BpcsBinVO> list = rows.stream().map(this::toBinVO).toList();
        return new PageResult<>(list.size(), list);
    }

    @Override
    public PageResult<BpcsBinInventoryVO> searchBinInventory(String cono, String whse, int current, int size) {
        if (profileResolver.isMockMode()) {
            return mockBinInventory(cono, whse, current, size);
        }
        String lib = configHolder.get(BpcsOrderServiceImpl.KEY_LIBRARY, "BPCSF");
        String sql = statements.get("bpcs.wms.binInventory").replace("{lib}", lib);
        List<Map<String, Object>> rows = clientProvider.current()
                .queryListCheckedBounded(sql, size,
                        cono, whse, size);
        List<BpcsBinInventoryVO> list = rows.stream().map(this::toBinInventoryVO).toList();
        return new PageResult<>(list.size(), list);
    }

    @Override
    public PageResult<BpcsMovementVO> searchMovements(BpcsMovementQueryDTO query) {
        if (profileResolver.isMockMode()) {
            return mockMovements(query);
        }
        String lib = configHolder.get(BpcsOrderServiceImpl.KEY_LIBRARY, "BPCSF");
        String sql = statements.get("bpcs.wms.movement").replace("{lib}", lib);
        List<Map<String, Object>> rows = clientProvider.current()
                .queryListCheckedBounded(sql, query.getSize(),
                        query.getCono(),
                        like(query.getWhse()),
                        like(query.getItem()),
                        query.getSize());
        List<BpcsMovementVO> list = rows.stream().map(this::toMovementVO).toList();
        return new PageResult<>(list.size(), list);
    }

    @Override
    public PageResult<BpcsBatchTrackingVO> searchBatches(BpcsBatchQueryDTO query) {
        if (profileResolver.isMockMode()) {
            return mockBatches(query);
        }
        String lib = configHolder.get(BpcsOrderServiceImpl.KEY_LIBRARY, "BPCSF");
        String sql = statements.get("bpcs.wms.batch").replace("{lib}", lib);
        List<Map<String, Object>> rows = clientProvider.current()
                .queryListCheckedBounded(sql, query.getSize(),
                        query.getCono(),
                        like(query.getLotno()),
                        query.getSize());
        List<BpcsBatchTrackingVO> list = rows.stream().map(this::toBatchTrackingVO).toList();
        return new PageResult<>(list.size(), list);
    }

    @Override
    public List<BpcsWarehouseSummaryVO> warehouseSummary(String cono) {
        if (profileResolver.isMockMode()) {
            return mockWarehouseSummary(cono);
        }
        String lib = configHolder.get(BpcsOrderServiceImpl.KEY_LIBRARY, "BPCSF");
        String sql = statements.get("bpcs.wms.summary").replace("{lib}", lib);
        List<Map<String, Object>> rows = clientProvider.current()
                .queryListCheckedBounded(sql, 100, cono);
        return rows.stream().map(this::toSummaryVO).toList();
    }

    // ── Mock 数据 ─────────────────────────────────────────────

    private PageResult<BpcsWarehouseVO> mockWarehouses(BpcsWarehouseQueryDTO query) {
        List<BpcsWarehouseVO> list = List.of(
                new BpcsWarehouseVO("001", "WH1", "主仓库", "A区"),
                new BpcsWarehouseVO("001", "WH2", "成品仓", "B区"),
                new BpcsWarehouseVO("001", "WH3", "原材料仓", "C区"));
        return new PageResult<>(list.size(), list);
    }

    private PageResult<BpcsBinVO> mockBins(BpcsBinQueryDTO query) {
        List<BpcsBinVO> list = List.of(
                new BpcsBinVO("001", "WH1", "A-01-01", "标准", "O", java.math.BigDecimal.valueOf(1000)),
                new BpcsBinVO("001", "WH1", "A-01-02", "标准", "F", java.math.BigDecimal.valueOf(1000)),
                new BpcsBinVO("001", "WH1", "A-02-01", "重型", "O", java.math.BigDecimal.valueOf(2000)),
                new BpcsBinVO("001", "WH2", "B-01-01", "标准", "O", java.math.BigDecimal.valueOf(1000)),
                new BpcsBinVO("001", "WH2", "B-01-02", "标准", "F", java.math.BigDecimal.valueOf(1000)));
        return new PageResult<>(list.size(), list);
    }

    private PageResult<BpcsBinInventoryVO> mockBinInventory(String cono, String whse, int current, int size) {
        List<BpcsBinInventoryVO> list = List.of(
                new BpcsBinInventoryVO("WH1", "A-01-01", "标准", "O", 1000, "ITEM-001", "螺丝M6", 500, "LOT-202501"),
                new BpcsBinInventoryVO("WH1", "A-01-01", "标准", "O", 1000, "ITEM-002", "垫圈M6", 200, "LOT-202501"),
                new BpcsBinInventoryVO("WH1", "A-02-01", "重型", "O", 2000, "ITEM-003", "钢板Q235", 100, "LOT-202502"),
                new BpcsBinInventoryVO("WH2", "B-01-01", "标准", "O", 1000, "ITEM-004", "成品A", 50, "LOT-202503"));
        return new PageResult<>(list.size(), list);
    }

    private PageResult<BpcsMovementVO> mockMovements(BpcsMovementQueryDTO query) {
        List<BpcsMovementVO> list = List.of(
                new BpcsMovementVO("001", "WH1", "ITEM-001", "A-01-01", "A-02-01", 100, "MV", "20250801", "143022", "REF-001", "ADMIN"),
                new BpcsMovementVO("001", "WH1", "ITEM-002", "A-01-02", "A-01-01", 50, "RC", "20250802", "091533", "REF-002", "OPER"),
                new BpcsMovementVO("001", "WH2", "ITEM-004", "B-01-01", "", 20, "SH", "20250803", "110045", "REF-003", "ADMIN"));
        return new PageResult<>(list.size(), list);
    }

    private PageResult<BpcsBatchTrackingVO> mockBatches(BpcsBatchQueryDTO query) {
        List<BpcsBatchTrackingVO> list = List.of(
                new BpcsBatchTrackingVO("WH1", "A-01-01", "ITEM-001", "螺丝M6", 500, "LOT-202501", "20250801", "RC", 500, "PO-001"),
                new BpcsBatchTrackingVO("WH1", "A-01-01", "ITEM-001", "螺丝M6", 400, "LOT-202501", "20250803", "SH", -100, "SO-001"));
        return new PageResult<>(list.size(), list);
    }

    private List<BpcsWarehouseSummaryVO> mockWarehouseSummary(String cono) {
        return List.of(
                new BpcsWarehouseSummaryVO("WH1", "主仓库", 20, 15, 5, 20000),
                new BpcsWarehouseSummaryVO("WH2", "成品仓", 15, 10, 5, 15000),
                new BpcsWarehouseSummaryVO("WH3", "原材料仓", 25, 20, 5, 50000));
    }

    // ── 行映射工具 ─────────────────────────────────────────────

    private BpcsWarehouseVO toWarehouseVO(Map<String, Object> row) {
        return new BpcsWarehouseVO(
                str(row, "CONO"), str(row, "WHSE"), str(row, "WHNAME"), str(row, "WHLOC"));
    }

    private BpcsBinVO toBinVO(Map<String, Object> row) {
        return new BpcsBinVO(
                str(row, "CONO"), str(row, "WHSE"), str(row, "BINNO"),
                str(row, "BINTYPE"), str(row, "STATUS"), num(row, "CAPACITY"));
    }

    private BpcsBinInventoryVO toBinInventoryVO(Map<String, Object> row) {
        return new BpcsBinInventoryVO(
                str(row, "WHSE"), str(row, "BINNO"), str(row, "BINTYPE"),
                str(row, "STATUS"), intNum(row, "CAPACITY"),
                str(row, "ITEM"), str(row, "ITDSC"),
                intNum(row, "QTY"), str(row, "LOTNO"));
    }

    private BpcsMovementVO toMovementVO(Map<String, Object> row) {
        return new BpcsMovementVO(
                str(row, "CONO"), str(row, "WHSE"), str(row, "ITEM"),
                str(row, "FROMBIN"), str(row, "TOBIN"), intNum(row, "QTY"),
                str(row, "ITTYP"), str(row, "TRNDATE"), str(row, "TRNTIME"),
                str(row, "REFNO"), str(row, "USERID"));
    }

    private BpcsBatchTrackingVO toBatchTrackingVO(Map<String, Object> row) {
        return new BpcsBatchTrackingVO(
                str(row, "WHSE"), str(row, "BINNO"), str(row, "ITEM"),
                str(row, "ITDSC"), intNum(row, "QTY"), str(row, "LOTNO"),
                str(row, "TRNDATE"), str(row, "ITTYP"),
                intNum(row, "TRNQTY"), str(row, "REFNO"));
    }

    private BpcsWarehouseSummaryVO toSummaryVO(Map<String, Object> row) {
        return new BpcsWarehouseSummaryVO(
                str(row, "WHSE"), str(row, "WHNAME"),
                intNum(row, "TOTAL_BINS"), intNum(row, "OCCUPIED"),
                intNum(row, "FREE"), intNum(row, "TOTAL_CAPACITY"));
    }

    private String str(Map<String, Object> row, String key) {
        Object v = row.get(key);
        return v == null ? "" : String.valueOf(v);
    }

    private int intNum(Map<String, Object> row, String key) {
        Object v = row.get(key);
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(v)); } catch (Exception e) { return 0; }
    }

    private java.math.BigDecimal num(Map<String, Object> row, String key) {
        Object v = row.get(key);
        if (v instanceof java.math.BigDecimal bd) return bd;
        try { return new java.math.BigDecimal(String.valueOf(v)); } catch (Exception e) { return java.math.BigDecimal.ZERO; }
    }

    private String like(String val) {
        return (val == null || val.isBlank()) ? "%" : "%" + val + "%";
    }
}
