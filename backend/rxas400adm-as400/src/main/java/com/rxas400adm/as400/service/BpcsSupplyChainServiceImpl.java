package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.dto.BpcsOrderListQueryDTO;
import com.rxas400adm.as400.sql.SqlStatementRegistry;
import com.rxas400adm.as400.util.BpcsRowUtil;
import com.rxas400adm.as400.vo.*;
import com.rxas400adm.common.config.ProfileResolver;
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
            return null;
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

    // ==================== Mock 数据 ====================

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
        if (v != null) { try { return Long.parseLong(String.valueOf(v).trim()); } catch (Exception ignored) {} }
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
