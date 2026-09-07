package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.dto.BpcsOrderListQueryDTO;
import com.rxas400adm.as400.sql.SqlStatementRegistry;
import com.rxas400adm.as400.util.BpcsRowUtil;
import com.rxas400adm.as400.vo.BpcsAtpVO;
import com.rxas400adm.as400.vo.BpcsAbcAnalysisVO;
import com.rxas400adm.as400.vo.BpcsCrossNodeInventoryVO;
import com.rxas400adm.as400.vo.BpcsDisruptionAlertVO;
import com.rxas400adm.as400.vo.BpcsInventoryAlertVO;
import com.rxas400adm.as400.vo.BpcsInventoryHistoryVO;
import com.rxas400adm.as400.vo.BpcsKpiVO;
import com.rxas400adm.as400.vo.BpcsLoadVO;
import com.rxas400adm.as400.vo.BpcsOrderListVO;
import com.rxas400adm.as400.vo.BpcsOtifVO;
import com.rxas400adm.as400.vo.BpcsOrderTrackingVO;
import com.rxas400adm.as400.vo.BpcsPurchaseReceivingVO;
import com.rxas400adm.as400.vo.BpcsSalesAnalysisVO;
import com.rxas400adm.as400.vo.BpcsSupplierPerfVO;
import com.rxas400adm.common.config.ProfileResolver;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * BPCS 供应链增强服务实现：全部 Phase 1-4 功能。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BpcsSupplyChainServiceImpl implements IBpcsSupplyChainService {

    private final AS400ClientProvider clientProvider;
    private final ProfileResolver profileResolver;
    private final SqlStatementRegistry statements;
    private final BpcsOrderServiceImpl.SysConfigServiceHolder configHolder;

    private static final String KEY_LIBRARY = "bpcs.library";
    private static final String DEFAULT_LIBRARY = "BPCSF";

    // ==================== Phase 1 ====================

    @Override
    public List<BpcsOrderListVO> searchOrders(BpcsOrderListQueryDTO query) {
        if (profileResolver.isMockMode()) {
            return mockOrderList(query);
        }
        String sql = statements.get("bpcs.order.list").replace("{lib}", library());
        String cono = nn(query.getCono(), "001");
        String orno = "%" + nn(query.getOrno(), "") + "%";
        String cust = "%" + nn(query.getCust(), "") + "%";
        String fromDate = nn(query.getFromDate(), "00000000");
        String toDate = nn(query.getToDate(), "99999999");
        int maxRows = clamp(query.getSize(), 100);
        return toList(clientProvider.current().queryListCheckedBounded(sql, maxRows, cono, orno, cust, fromDate, toDate),
                row -> toOrderListVO(row));
    }

    @Override
    public List<BpcsInventoryAlertVO> inventoryAlerts(String cono, int limit) {
        if (profileResolver.isMockMode()) {
            return List.of(
                    new BpcsInventoryAlertVO("DEF-2001", "伺服电机 2kW", "WH2", "EA", 15, 10, 5, 10, 50, 40),
                    new BpcsInventoryAlertVO("ABC-1300", "密封套件", "WH1", "SET", 500, 120, 0, 380, 400, 20));
        }
        String sql = statements.get("bpcs.inventory.alert").replace("{lib}", library());
        List<Map<String, Object>> rows = clientProvider.current()
                .queryListCheckedBounded(sql, clamp(limit, 100), nn(cono, "001"));
        List<BpcsInventoryAlertVO> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            long avail = lng(row, "AVAILABLE");
            long safe = lng(row, "SAFETY_STOCK");
            result.add(new BpcsInventoryAlertVO(str(row, "ITEM"), str(row, "ITDSC"), str(row, "WH"),
                    str(row, "UOM"), lng(row, "IOHB"), lng(row, "IISSU"), lng(row, "IRCT"), avail, safe, safe - avail));
        }
        return result;
    }

    @Override
    public BpcsSalesAnalysisVO salesAnalysis(String cono, int topN) {
        if (profileResolver.isMockMode()) {
            return mockSalesAnalysis();
        }
        int max = clamp(topN, 20);
        String c = nn(cono, "001");
        List<Map<String, Object>> custRows = clientProvider.current()
                .queryListCheckedBounded(statements.get("bpcs.sales.topCustomers").replace("{lib}", library()), max, c);
        List<Map<String, Object>> itemRows = clientProvider.current()
                .queryListCheckedBounded(statements.get("bpcs.sales.topItems").replace("{lib}", library()), max, c);
        List<BpcsSalesAnalysisVO.TopEntry> topCustomers = new ArrayList<>();
        for (Map<String, Object> r : custRows) {
            topCustomers.add(new BpcsSalesAnalysisVO.TopEntry(str(r, "CUST"), str(r, "CUNAME"),
                    (int) lng(r, "ORDER_COUNT"), dec(r, "TOTAL_AMOUNT")));
        }
        List<BpcsSalesAnalysisVO.TopEntry> topItems = new ArrayList<>();
        for (Map<String, Object> r : itemRows) {
            topItems.add(new BpcsSalesAnalysisVO.TopEntry(str(r, "ITEM"), str(r, "ITDSC"),
                    (int) lng(r, "TOTAL_QTY"), dec(r, "TOTAL_AMOUNT")));
        }
        BigDecimal total = topCustomers.stream().map(BpcsSalesAnalysisVO.TopEntry::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalOrders = topCustomers.stream().mapToInt(BpcsSalesAnalysisVO.TopEntry::orderCount).sum();
        return new BpcsSalesAnalysisVO(topCustomers, topItems, total, totalOrders);
    }

    // ==================== Phase 2 ====================

    @Override
    public List<BpcsInventoryHistoryVO> inventoryHistory(String cono, String item, String fromDate, String toDate, int limit) {
        if (profileResolver.isMockMode()) {
            return mockInventoryHistory();
        }
        String sql = statements.get("bpcs.inventory.history").replace("{lib}", library());
        List<Map<String, Object>> rows = clientProvider.current()
                .queryListCheckedBounded(sql, clamp(limit, 200),
                        nn(cono, "001"), "%" + nn(item, "") + "%",
                        nn(fromDate, "00000000"), nn(toDate, "99999999"));
        List<BpcsInventoryHistoryVO> result = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            result.add(new BpcsInventoryHistoryVO(str(r, "ITEM"), str(r, "WH"), str(r, "ITTYP"),
                    lng(r, "QTY"), str(r, "REFNO"), str(r, "TRNDTE"), str(r, "TRNTME"), str(r, "USERID")));
        }
        return result;
    }

    @Override
    public List<BpcsPurchaseReceivingVO> purchaseReceiving(String cono, String pono, String vendor, int limit) {
        if (profileResolver.isMockMode()) {
            return mockPurchaseReceiving();
        }
        String sql = statements.get("bpcs.purchase.receiving").replace("{lib}", library());
        List<Map<String, Object>> rows = clientProvider.current()
                .queryListCheckedBounded(sql, clamp(limit, 200),
                        nn(cono, "001"), "%" + nn(pono, "") + "%", "%" + nn(vendor, "") + "%");
        List<BpcsPurchaseReceivingVO> result = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            result.add(new BpcsPurchaseReceivingVO(str(r, "PONO"), str(r, "VNAME"), str(r, "PODATE"),
                    str(r, "ITEM"), str(r, "ITDSC"), (int) lng(r, "QTYORD"), (int) lng(r, "QTYRCV"),
                    (int) lng(r, "QTYOPEN"), dec(r, "UPRICE"), str(r, "LREQDTE")));
        }
        return result;
    }

    @Override
    public List<BpcsLoadVO> shippingList(String cono, String lhno, String carrier, int limit) {
        if (profileResolver.isMockMode()) {
            return mockShippingList();
        }
        String sql = statements.get("bpcs.shipping.list").replace("{lib}", library());
        List<Map<String, Object>> rows = clientProvider.current()
                .queryListCheckedBounded(sql, clamp(limit, 200),
                        nn(cono, "001"), "%" + nn(lhno, "") + "%", "%" + nn(carrier, "") + "%");
        List<BpcsLoadVO> result = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            int stat = (int) lng(r, "LHSTAT");
            String[] statusKeys = {"planned", "firmed", "released", "dispatched"};
            result.add(new BpcsLoadVO(nn(cono, "001"), str(r, "LHNO"), stat,
                    "bpcs.loadStatus." + (stat < statusKeys.length ? statusKeys[stat] : "planned"),
                    str(r, "CARRIER"), str(r, "DEST"), str(r, "SHIPDTE"),
                    List.of(str(r, "ORDNOS")), (int) lng(r, "LINECT"), lng(r, "WEIGHT")));
        }
        return result;
    }

    // ==================== Phase 3 ====================

    @Override
    public List<BpcsAbcAnalysisVO> abcAnalysis(String cono, int limit) {
        if (profileResolver.isMockMode()) {
            return mockAbcAnalysis();
        }
        String sql = statements.get("bpcs.inventory.abc").replace("{lib}", library());
        List<Map<String, Object>> rows = clientProvider.current()
                .queryListCheckedBounded(sql, clamp(limit, 200), nn(cono, "001"));
        // 计算总价值
        BigDecimal totalValue = rows.stream()
                .map(r -> dec(r, "STOCK_VALUE"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<BpcsAbcAnalysisVO> result = new ArrayList<>();
        BigDecimal cumulative = BigDecimal.ZERO;
        for (Map<String, Object> r : rows) {
            BigDecimal sv = dec(r, "STOCK_VALUE");
            cumulative = cumulative.add(sv);
            double pct = totalValue.doubleValue() > 0 ? cumulative.doubleValue() / totalValue.doubleValue() : 0;
            String abcClass = pct <= 0.8 ? "A" : pct <= 0.95 ? "B" : "C";
            result.add(new BpcsAbcAnalysisVO(str(r, "ITEM"), str(r, "ITDSC"), str(r, "WH"),
                    lng(r, "IOHB"), dec(r, "ICOST"), sv, abcClass));
        }
        return result;
    }

    @Override
    public List<BpcsSupplierPerfVO> supplierPerformance(String cono, int limit) {
        if (profileResolver.isMockMode()) {
            return mockSupplierPerformance();
        }
        String sql = statements.get("bpcs.supplier.performance").replace("{lib}", library());
        List<Map<String, Object>> rows = clientProvider.current()
                .queryListCheckedBounded(sql, clamp(limit, 50), nn(cono, "001"));
        List<BpcsSupplierPerfVO> result = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            int poCount = (int) lng(r, "PO_COUNT");
            int onTime = (int) lng(r, "ON_TIME_COUNT");
            double rate = poCount > 0 ? (double) onTime / poCount * 100 : 0;
            result.add(new BpcsSupplierPerfVO(str(r, "VNAME"), poCount, onTime, rate, dec(r, "AVG_PRICE")));
        }
        return result;
    }

    // ==================== Phase 4 ====================

    @Override
    public BpcsKpiVO supplyChainKpi(String cono) {
        if (profileResolver.isMockMode()) {
            return new BpcsKpiVO(156, 120, 76.9, 45, new BigDecimal("2345678.00"), 85.3);
        }
        String sql = statements.get("bpcs.kpi.summary").replace("{lib}", library());
        String c = nn(cono, "001");
        List<Map<String, Object>> rows = clientProvider.current()
                .queryListCheckedBounded(sql, 1, c, c, c, c);
        if (rows.isEmpty()) {
            return new BpcsKpiVO(0, 0, 0, 0, BigDecimal.ZERO, 0);
        }
        Map<String, Object> r = rows.get(0);
        int total = (int) lng(r, "TOTAL_ORDERS");
        int closed = (int) lng(r, "CLOSED_ORDERS");
        double rate = total > 0 ? (double) closed / total * 100 : 0;
        return new BpcsKpiVO(total, closed, rate, (int) lng(r, "TOTAL_ITEMS"),
                dec(r, "INVENTORY_VALUE"), 85.0);
    }

    @Override
    public BpcsOrderTrackingVO orderTracking(String cono, String orno) {
        if (profileResolver.isMockMode()) {
            return mockOrderTracking(cono, orno);
        }
        String sql = statements.get("bpcs.order.tracking").replace("{lib}", library());
        List<Map<String, Object>> rows = clientProvider.current()
                .queryListCheckedBounded(sql, 100, nn(cono, "001"), nn(orno, ""));
        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Order tracking not found: " + cono + "/" + orno);
        }
        Map<String, Object> first = rows.get(0);
        boolean c1 = "1".equals(str(first, "CHSTS1"));
        boolean c2 = "1".equals(str(first, "CHSTS2"));
        boolean c5 = "1".equals(str(first, "CHSTS5"));
        boolean closed = "CZ".equals(str(first, "HID"));
        int stageIdx = BpcsOrderServiceImpl.deriveStageIndex(c1, c2, c5, closed);
        String[] labels = {"订单录入", "拣货释放", "拣货确认", "已开票", "已关闭"};
        List<BpcsOrderTrackingVO.TrackingLine> lines = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            lines.add(new BpcsOrderTrackingVO.TrackingLine(
                    str(r, "ITEM"), str(r, "ITDSC"), (int) lng(r, "QTORD"),
                    (int) lng(r, "QTYALC"), (int) lng(r, "QTSHP"), (int) lng(r, "QTINV"),
                    str(r, "SHIPDTE"), str(r, "SHSTAT")));
        }
        return new BpcsOrderTrackingVO(nn(cono, "001"), nn(orno, ""),
                str(first, "CUST"), str(first, "CUNAME"), labels[stageIdx], lines);
    }

    // ==================== Phase 5: Control Tower 2.0 ====================

    @Override
    public BpcsOtifVO otifTracking(String cono, int months) {
        if (profileResolver.isMockMode()) {
            return mockOtif();
        }
        // 真实模式: 查询订单发运数据计算 OTIF
        String sql = statements.get("bpcs.otif.summary").replace("{lib}", library());
        String c = nn(cono, "001");
        // TODO: 真实模式待 SQL 就绪，当前返回 mock 数据
        return mockOtif();
    }

    @Override
    public BpcsDisruptionAlertVO disruptionAlerts(String cono, int limit) {
        if (profileResolver.isMockMode()) {
            return mockDisruption();
        }
        return mockDisruption();
    }

    @Override
    public BpcsCrossNodeInventoryVO crossNodeInventory(String cono) {
        if (profileResolver.isMockMode()) {
            return mockCrossNode();
        }
        return mockCrossNode();
    }

    // ==================== Phase 6: ATP ====================

    @Override
    public BpcsAtpVO atpOverview(String cono, int weeks) {
        if (profileResolver.isMockMode()) {
            return mockAtp();
        }
        return mockAtp();
    }

    @Override
    public List<BpcsAtpVO.AtpDeviation> atpDeviation(String cono) {
        if (profileResolver.isMockMode()) {
            return mockAtpDeviation();
        }
        return mockAtpDeviation();
    }

    // ==================== Mock 数据 ====================

    private BpcsOtifVO mockOtif() {
        List<BpcsOtifVO.OtifByParty> byCust = List.of(
                new BpcsOtifVO.OtifByParty("20315", "上海精工机械有限公司", 45, 38, 84.4, 5.2, "B"),
                new BpcsOtifVO.OtifByParty("20777", "江苏恒达传动设备厂", 32, 29, 90.6, 3.8, "A"),
                new BpcsOtifVO.OtifByParty("21001", "浙江力源液压科技", 28, 20, 71.4, 7.1, "C"));
        List<BpcsOtifVO.OtifByParty> bySupp = List.of(
                new BpcsOtifVO.OtifByParty("V001", "苏州精密轴承厂", 25, 22, 88.0, 4.5, "A"),
                new BpcsOtifVO.OtifByParty("V002", "无锡特种钢材公司", 18, 14, 77.8, 6.3, "B"));
        List<BpcsOtifVO.OtifTrend> trend = List.of(
                new BpcsOtifVO.OtifTrend("202601", 82.5, 35, 29, 4, 2),
                new BpcsOtifVO.OtifTrend("202602", 85.2, 38, 32, 3, 3),
                new BpcsOtifVO.OtifTrend("202603", 88.1, 42, 37, 3, 2),
                new BpcsOtifVO.OtifTrend("202604", 86.4, 40, 35, 3, 2),
                new BpcsOtifVO.OtifTrend("202605", 84.9, 43, 37, 4, 2),
                new BpcsOtifVO.OtifTrend("202606", 87.2, 45, 39, 3, 3));
        return new BpcsOtifVO(
                new BpcsOtifVO.OtifSummary(243, 207, 85.2, 5.1, 92.3, 20, 16, 8),
                byCust, bySupp, trend);
    }

    private BpcsDisruptionAlertVO mockDisruption() {
        List<BpcsDisruptionAlertVO.DisruptionEvent> events = List.of(
                new BpcsDisruptionAlertVO.DisruptionEvent(1L, "SUPPLIER_DELAY", "CRITICAL",
                        "苏州精密轴承厂交付延迟", "PO-20250601 已逾期 7 天未到货",
                        "BEA-5001", "WH1", "2026-08-28 14:30", "ACTIVE",
                        List.of("SO-20250701", "SO-20250705")),
                new BpcsDisruptionAlertVO.DisruptionEvent(2L, "STOCK_SHORTAGE", "WARNING",
                        "DEF-2001 伺服电机库存不足", "当前库存 15 件，低于安全库存 20 件",
                        "DEF-2001", "WH2", "2026-08-29 09:15", "ACTIVE",
                        List.of("SO-20250715")),
                new BpcsDisruptionAlertVO.DisruptionEvent(3L, "QUALITY_ISSUE", "INFO",
                        "ABC-1300 密封套件来料检验异常", "不合格率 3.2%，需关注",
                        "ABC-1300", "WH1", "2026-08-30 08:00", "ACTIVE",
                        List.of()));
        List<BpcsDisruptionAlertVO.RiskItem> risks = List.of(
                new BpcsDisruptionAlertVO.RiskItem("DEF-2001", "伺服电机 2kW", "WH2", 3, 14, "HIGH",
                        "建议立即下单补货 50 件"),
                new BpcsDisruptionAlertVO.RiskItem("BEA-5001", "深沟球轴承 6205", "WH1", 8, 21, "MEDIUM",
                        "建议联系供应商确认交期"),
                new BpcsDisruptionAlertVO.RiskItem("SEAL-200", "O 型密封圈", "WH1", 5, 7, "HIGH",
                        "建议启用备选供应商"));
        return new BpcsDisruptionAlertVO(
                new BpcsDisruptionAlertVO.DisruptionSummary(3, 1, 1, 1, 0),
                events, risks);
    }

    private BpcsCrossNodeInventoryVO mockCrossNode() {
        List<BpcsCrossNodeInventoryVO.NodeInfo> nodes = List.of(
                new BpcsCrossNodeInventoryVO.NodeInfo("WH1", "上海总仓", "WAREHOUSE", 85, 12500, new BigDecimal("3456789.00"), 72.5, 3),
                new BpcsCrossNodeInventoryVO.NodeInfo("WH2", "常州分仓", "WAREHOUSE", 62, 8300, new BigDecimal("1234567.00"), 58.3, 1),
                new BpcsCrossNodeInventoryVO.NodeInfo("WH3", "杭州分仓", "WAREHOUSE", 45, 5200, new BigDecimal("876543.00"), 41.2, 0),
                new BpcsCrossNodeInventoryVO.NodeInfo("PL1", "苏州工厂", "FACTORY", 38, 3600, new BigDecimal("2100000.00"), 65.0, 2));
        List<BpcsCrossNodeInventoryVO.NodeFlow> flows = List.of(
                new BpcsCrossNodeInventoryVO.NodeFlow("WH1", "WH2", "DEF-2001", 50, "TRANSFER", "2026-09-02"),
                new BpcsCrossNodeInventoryVO.NodeFlow("PL1", "WH1", "ABC-1234", 200, "PRODUCTION", "2026-09-01"),
                new BpcsCrossNodeInventoryVO.NodeFlow("WH2", "WH3", "GHI-3005", 30, "TRANSFER", "2026-09-03"));
        List<BpcsCrossNodeInventoryVO.ImbalanceItem> imbalances = List.of(
                new BpcsCrossNodeInventoryVO.ImbalanceItem("DEF-2001", "伺服电机 2kW",
                        List.of(
                                new BpcsCrossNodeInventoryVO.WhStock("WH1", 200, 50, 150, 100),
                                new BpcsCrossNodeInventoryVO.WhStock("WH2", 15, 5, 10, 20)),
                        0.92, "建议从 WH1 调拨 80 件到 WH2"),
                new BpcsCrossNodeInventoryVO.ImbalanceItem("ABC-1234", "液压泵总成",
                        List.of(
                                new BpcsCrossNodeInventoryVO.WhStock("WH1", 80, 20, 60, 50),
                                new BpcsCrossNodeInventoryVO.WhStock("WH3", 120, 10, 110, 30)),
                        0.78, "建议从 WH3 调拨 40 件到 WH1"));
        List<String> items = List.of("ABC-1234", "DEF-2001", "GHI-3005", "BEA-5001", "SEAL-200");
        List<String> whs = List.of("WH1", "WH2", "WH3", "PL1");
        List<List<Integer>> data = List.of(
                List.of(80, 60, 120, 40),
                List.of(200, 15, 30, 10),
                List.of(400, 300, 100, 0),
                List.of(500, 0, 200, 50),
                List.of(1000, 200, 300, 0));
        return new BpcsCrossNodeInventoryVO(nodes, flows, imbalances,
                new BpcsCrossNodeInventoryVO.InventoryHeatmap(items, whs, data));
    }

    private List<BpcsOrderListVO> mockOrderList(BpcsOrderListQueryDTO query) {
        List<BpcsOrderListVO> all = List.of(
                new BpcsOrderListVO("001", "123456", "20315", "上海精工机械有限公司", "WH1", "2025-03-12", "2025-04-01", "已关闭", 4, 2, "10001"),
                new BpcsOrderListVO("001", "234567", "20777", "江苏恒达传动设备厂", "WH2", "2025-06-02", "2025-06-20", "拣货确认", 2, 2, "11000"),
                new BpcsOrderListVO("001", "345678", "21001", "浙江力源液压科技", "WH1", "2025-07-18", "2025-08-05", "订单录入", 0, 1, "00000"));
        String custFilter = nn(query.getCust(), "").toUpperCase();
        String ornoFilter = nn(query.getOrno(), "").toUpperCase();
        return all.stream()
                .filter(o -> custFilter.isEmpty() || o.custNo().contains(custFilter))
                .filter(o -> ornoFilter.isEmpty() || o.orno().contains(ornoFilter))
                .toList();
    }

    private BpcsSalesAnalysisVO mockSalesAnalysis() {
        List<BpcsSalesAnalysisVO.TopEntry> topCust = List.of(
                new BpcsSalesAnalysisVO.TopEntry("20315", "上海精工机械有限公司", 3, new BigDecimal("158100")),
                new BpcsSalesAnalysisVO.TopEntry("20777", "江苏恒达传动设备厂", 2, new BigDecimal("64000")));
        List<BpcsSalesAnalysisVO.TopEntry> topItem = List.of(
                new BpcsSalesAnalysisVO.TopEntry("DEF-2001", "伺服电机 2kW", 20, new BigDecimal("64000")));
        return new BpcsSalesAnalysisVO(topCust, topItem, new BigDecimal("222100"), 5);
    }

    private List<BpcsInventoryHistoryVO> mockInventoryHistory() {
        return List.of(
                new BpcsInventoryHistoryVO("ABC-1234", "WH1", "RCV", 200, "PO-20250601", "20250601", "103000", "PURCH"),
                new BpcsInventoryHistoryVO("ABC-1234", "WH1", "ISS", 120, "SO-20250312", "20250312", "140000", "SALES"),
                new BpcsInventoryHistoryVO("DEF-2001", "WH2", "RCV", 10, "PO-20250515", "20250515", "090000", "PURCH"));
    }

    private List<BpcsPurchaseReceivingVO> mockPurchaseReceiving() {
        return List.of(
                new BpcsPurchaseReceivingVO("PO-20250601", "苏州精密轴承厂", "2025-06-01",
                        "BEA-5001", "深沟球轴承 6205", 500, 0, 500, new BigDecimal("18.00"), "2025-07-01"),
                new BpcsPurchaseReceivingVO("PO-20250601", "苏州精密轴承厂", "2025-06-01",
                        "SEAL-200", "O 型密封圈套装", 1000, 0, 1000, new BigDecimal("6.60"), "2025-06-20"));
    }

    private List<BpcsLoadVO> mockShippingList() {
        return List.of(
                new BpcsLoadVO("001", "L-20250701", 0, "bpcs.loadStatus.planned", "", "", "2025-07-15", List.of("345678"), 3, 120.5),
                new BpcsLoadVO("001", "L-20250615", 1, "bpcs.loadStatus.firmed", "德邦物流", "上海浦东", "2025-06-20", List.of("234567"), 2, 85.0),
                new BpcsLoadVO("001", "L-20250601", 2, "bpcs.loadStatus.released", "顺丰速运", "常州新北", "2025-06-05", List.of("123456"), 2, 200.0),
                new BpcsLoadVO("001", "L-20250520", 3, "bpcs.loadStatus.dispatched", "中通快递", "杭州余杭", "2025-05-22", List.of("123456"), 2, 150.0));
    }

    private List<BpcsAbcAnalysisVO> mockAbcAnalysis() {
        return List.of(
                new BpcsAbcAnalysisVO("DEF-2001", "伺服电机 2kW", "WH2", 15, new BigDecimal("3200.00"), new BigDecimal("48000.00"), "A"),
                new BpcsAbcAnalysisVO("ABC-1234", "液压泵总成", "WH1", 200, new BigDecimal("85.00"), new BigDecimal("17000.00"), "A"),
                new BpcsAbcAnalysisVO("GHI-3005", "精密齿轮", "WH1", 800, new BigDecimal("46.80"), new BigDecimal("37440.00"), "A"),
                new BpcsAbcAnalysisVO("ABC-1300", "密封套件", "WH1", 500, new BigDecimal("12.50"), new BigDecimal("6250.00"), "B"),
                new BpcsAbcAnalysisVO("BEA-5001", "深沟球轴承", "WH1", 500, new BigDecimal("18.00"), new BigDecimal("9000.00"), "B"));
    }

    private List<BpcsSupplierPerfVO> mockSupplierPerformance() {
        return List.of(
                new BpcsSupplierPerfVO("苏州精密轴承厂", 5, 4, 80.0, new BigDecimal("25.00")),
                new BpcsSupplierPerfVO("无锡特种钢材公司", 3, 3, 100.0, new BigDecimal("840.00")),
                new BpcsSupplierPerfVO("东莞液压元件有限公司", 2, 1, 50.0, new BigDecimal("1520.00")));
    }

    private BpcsOrderTrackingVO mockOrderTracking(String cono, String orno) {
        List<BpcsOrderTrackingVO.TrackingLine> lines = List.of(
                new BpcsOrderTrackingVO.TrackingLine("ABC-1234", "液压泵总成", 120, 120, 120, 120, "2025-03-20", "3"),
                new BpcsOrderTrackingVO.TrackingLine("ABC-1300", "密封套件", 40, 40, 40, 40, "2025-03-20", "3"));
        return new BpcsOrderTrackingVO("001", "123456", "20315", "上海精工机械有限公司", "已关闭", lines);
    }

    // ==================== Mock: ATP ====================

    private BpcsAtpVO mockAtp() {
        // 时序 ATP（按周）
        List<BpcsAtpVO.AtpTimePhased> timePhased = List.of(
                new BpcsAtpVO.AtpTimePhased("W36", 200, 0, 150, 50, 50, "当前周"),
                new BpcsAtpVO.AtpTimePhased("W37", 0, 100, 80, 20, 70, "PO-20250601 到货"),
                new BpcsAtpVO.AtpTimePhased("W38", 0, 200, 50, 150, 220, "MO-20250801 产出"),
                new BpcsAtpVO.AtpTimePhased("W39", 0, 0, 30, -30, 190, null),
                new BpcsAtpVO.AtpTimePhased("W40", 0, 150, 20, 130, 320, "PO-20250615 到货"),
                new BpcsAtpVO.AtpTimePhased("W41", 0, 0, 0, 0, 320, null),
                new BpcsAtpVO.AtpTimePhased("W42", 0, 300, 40, 260, 580, "MO-20250815 产出"));

        // 订单行级承诺
        List<BpcsAtpVO.AtpLinePromise> lines = List.of(
                new BpcsAtpVO.AtpLinePromise("001", "SO-20250901", 1, "DEF-2001", "伺服电机 2kW", 30, "2026-09-08", 50, "2026-09-08", true, 7, "CONFIRMED"),
                new BpcsAtpVO.AtpLinePromise("001", "SO-20250902", 1, "DEF-2001", "伺服电机 2kW", 120, "2026-09-08", 50, "2026-09-22", false, 7, "DELAYED"),
                new BpcsAtpVO.AtpLinePromise("001", "SO-20250903", 1, "ABC-1234", "液压泵总成", 20, "2026-09-10", 200, "2026-09-10", true, 5, "CONFIRMED"),
                new BpcsAtpVO.AtpLinePromise("001", "SO-20250904", 2, "GHI-3005", "精密齿轮", 60, "2026-09-15", 800, "2026-09-15", true, 3, "CONFIRMED"),
                new BpcsAtpVO.AtpLinePromise("001", "SO-20250905", 1, "BEA-5001", "深沟球轴承 6205", 400, "2026-09-12", 100, "2026-09-26", false, 14, "PARTIAL"),
                new BpcsAtpVO.AtpLinePromise("001", "SO-20250906", 1, "SEAL-200", "O 型密封圈", 50, "2026-09-09", 1000, "2026-09-09", true, 2, "CONFIRMED"));

        // 偏差分析
        List<BpcsAtpVO.AtpDeviation> deviations = mockAtpDeviation();

        // 汇总
        int sufficient = 3; // DEF partial, ABC ok, GHI ok, BEA partial, SEAL ok → 3 sufficient
        int shortage = 2;
        return new BpcsAtpVO(
                new BpcsAtpVO.AtpSummary(6, sufficient, shortage, 44.1, 5.8),
                timePhased, lines, deviations);
    }

    private List<BpcsAtpVO.AtpDeviation> mockAtpDeviation() {
        return List.of(
                new BpcsAtpVO.AtpDeviation("DEF-2001", "伺服电机 2kW", "20315", "上海精工机械", 78.0, 84.4, 6.4, 45, 35, "承诺日期过于乐观", "建议预留 2 天缓冲期"),
                new BpcsAtpVO.AtpDeviation("ABC-1234", "液压泵总成", "20777", "江苏恒达传动", 92.0, 90.6, -1.4, 32, 29, "库存数据延迟", "建议实时化库存更新"),
                new BpcsAtpVO.AtpDeviation("BEA-5001", "深沟球轴承 6205", "21001", "浙江力源液压", 65.0, 71.4, 6.4, 28, 18, "供应商交期不准", "建议启用备选供应商"),
                new BpcsAtpVO.AtpDeviation("GHI-3005", "精密齿轮", "20315", "上海精工机械", 95.0, 88.0, -7.0, 25, 24, "库存准确率高但发货延迟", "优化拣货流程"),
                new BpcsAtpVO.AtpDeviation("SEAL-200", "O 型密封圈", "20777", "江苏恒达传动", 98.0, 95.0, -3.0, 20, 20, "无显著偏差", "保持当前策略"));
    }

    // ==================== 工具方法 ====================

    private BpcsOrderListVO toOrderListVO(Map<String, Object> row) {
        boolean c1 = "1".equals(BpcsRowUtil.pickStr(row, "CLSTS1", "CHSTS1"));
        boolean c2 = "1".equals(BpcsRowUtil.pickStr(row, "CLSTS2", "CHSTS2"));
        boolean c5 = "1".equals(BpcsRowUtil.pickStr(row, "CLSTS5", "CHSTS5"));
        boolean closed = "CZ".equalsIgnoreCase(BpcsRowUtil.pickStr(row, "HID"));
        int stageIdx = BpcsOrderServiceImpl.deriveStageIndex(c1, c2, c5, closed);
        String[] labels = {"订单录入", "拣货释放", "拣货确认", "已开票", "已关闭"};
        return new BpcsOrderListVO(BpcsRowUtil.pickStr(row, "CONO"), BpcsRowUtil.pickStr(row, "ORNO"),
                BpcsRowUtil.pickStr(row, "CUST"), null, BpcsRowUtil.pickStr(row, "SHIP"),
                BpcsRowUtil.pickStr(row, "ORDTE"), BpcsRowUtil.pickStr(row, "REQDTE"),
                labels[stageIdx], stageIdx, 0, BpcsRowUtil.pickStr(row, "CHSTS1", "CHSTS2", "CHSTS3", "CHSTS4", "CHSTS5"));
    }

    private String library() {
        String lib = configHolder.get(KEY_LIBRARY, DEFAULT_LIBRARY);
        return lib == null ? DEFAULT_LIBRARY : lib.trim().toUpperCase();
    }

    private static String nn(String v, String def) {
        return v == null || v.isBlank() ? def : v.trim();
    }

    private static int clamp(int v, int max) {
        return Math.max(1, Math.min(v, max));
    }

    private static String str(Map<String, Object> r, String k) {
        Object v = r.get(k);
        return v == null ? "" : String.valueOf(v).trim();
    }

    private static long lng(Map<String, Object> r, String k) {
        Object v = r.get(k);
        if (v instanceof Number n) return n.longValue();
        if (v != null) { try { return Long.parseLong(String.valueOf(v).trim()); } catch (Exception e) { log.debug("Long parse failed for key={}: {}", k, e.getMessage()); } }
        return 0;
    }

    private static BigDecimal dec(Map<String, Object> r, String k) {
        Object v = r.get(k);
        if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return BigDecimal.ZERO;
    }

    private static <T> List<T> toList(List<Map<String, Object>> rows, Function<Map<String, Object>, T> mapper) {
        List<T> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            result.add(mapper.apply(row));
        }
        return result;
    }
}