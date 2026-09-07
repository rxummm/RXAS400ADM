package com.rxas400adm.report.builder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.report.IReportService;
import com.rxas400adm.report.builder.dto.ReportDefinitionDTO;
import com.rxas400adm.report.builder.mapper.ReportDefinitionMapper;
import com.rxas400adm.report.builder.vo.DataSourceMeta;
import com.rxas400adm.report.builder.vo.DataSourceMeta.FieldMeta;
import com.rxas400adm.report.builder.vo.ReportDefinitionVO;
import com.rxas400adm.as400.dto.*;
import com.rxas400adm.as400.service.*;
import com.rxas400adm.as400.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.reflect.RecordComponent;
import java.util.*;

/**
 * 自定义报表构建器服务实现。
 * <p>
 * 架构决策：采用<strong>服务级方案</strong>——复用现有 BPCS Service 查询数据，
 * 在内存中做字段投影/筛选/排序，避免动态 SQL 注入风险。
 * 每个数据源对应一个 DataSourceFetcher（函数式接口），返回 {@code List<Map<String, Object>>}。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportBuilderService implements IReportBuilderService {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<ColumnDef>> COL_REF = new TypeReference<>() {};
    private static final TypeReference<List<FilterDef>> FIL_REF = new TypeReference<>() {};
    private static final TypeReference<List<SortDef>> SOR_REF = new TypeReference<>() {};

    private final ReportDefinitionMapper definitionMapper;
    private final IBpcsCustomerService customerService;
    private final IBpcsItemService itemService;
    private final IBpcsInventoryService inventoryService;
    private final IBpcsPurchaseService purchaseService;
    private final IBpcsShippingService shippingService;
    private final IBpcsInvoiceService invoiceService;
    private final IBpcsSalesService salesService;
    private final IBpcsSupplyChainService supplyChainService;
    private final IReportService reportService;

    // ── 数据源注册表 ──────────────────────────────────────────────

    private record ColumnDef(String key, String label, String type) {}
    private record FilterDef(String field, String op, String value) {}
    private record SortDef(String field, boolean asc) {}

    @FunctionalInterface
    private interface DataSourceFetcher {
        List<Map<String, Object>> fetch(String cono);
    }

    /** 数据源注册表：key → (label, fields, fetcher) */
    private record DataSourceEntry(String label, List<FieldMeta> fields, DataSourceFetcher fetcher) {}

    private Map<String, DataSourceEntry> buildRegistry() {
        Map<String, DataSourceEntry> r = new LinkedHashMap<>();

        // ── 订单 ──
        r.put("orders", new DataSourceEntry("客户订单",
                List.of(
                        f("cono","公司代码","dimension"), f("orno","订单号","dimension"),
                        f("custNo","客户号","dimension"), f("custName","客户名称","dimension"),
                        f("shipTo","发货地","dimension"), f("orderDate","订单日期","date"),
                        f("reqDate","需求日期","date"), f("statusLabel","状态","dimension"),
                        f("lineCount","行数","measure")),
                cono -> {
                    List<BpcsOrderListVO> list = supplyChainService.searchOrders(new BpcsOrderListQueryDTO() {{ setCono(cono); }});
                    return list.stream().map(ReportBuilderService::toMap).toList();
                }));

        // ── 库存 ──
        r.put("inventory", new DataSourceEntry("库存查询",
                List.of(
                        f("item","物料编码","dimension"), f("description","物料描述","dimension"),
                        f("uom","单位","dimension"), f("totalOnHand","在库数量","measure"),
                        f("totalAllocated","已分配","measure"), f("totalOnOrder","在途数量","measure"),
                        f("totalAvailable","可用数量","measure"), f("unitCost","单位成本","measure")),
                cono -> {
                    PageResult<BpcsInventoryVO> page = inventoryService.search(new BpcsInventoryQueryDTO() {{ setCono(cono); setCurrent(1); setSize(10000); }});
                    return page.getRecords().stream().map(ReportBuilderService::toMap).toList();
                }));

        // ── 物料 ──
        r.put("items", new DataSourceEntry("物料主数据",
                List.of(
                        f("item","物料编码","dimension"), f("description","物料描述","dimension"),
                        f("uom","单位","dimension"), f("category","分类","dimension"),
                        f("unitCost","单位成本","measure"), f("listPrice","列表价","measure"),
                        f("weight","重量","measure"), f("shelfLife","保质期(天)","measure"),
                        f("totalOnHand","总在库","measure"), f("totalAvailable","总可用","measure")),
                cono -> {
                    PageResult<BpcsItemVO> page = itemService.search(new BpcsItemQueryDTO() {{ setCono(cono); setCurrent(1); setSize(10000); }});
                    return page.getRecords().stream().map(ReportBuilderService::toMap).toList();
                }));

        // ── 客户 ──
        r.put("customers", new DataSourceEntry("客户主数据",
                List.of(
                        f("cono","公司代码","dimension"), f("cust","客户号","dimension"),
                        f("name","客户名称","dimension"), f("city","城市","dimension"),
                        f("state","省份","dimension"), f("salesArea","销售区域","dimension"),
                        f("creditLimit","信用额度","measure"), f("termsCode","付款条件","dimension")),
                cono -> {
                    PageResult<BpcsCustomerVO> page = customerService.search(new BpcsCustomerQueryDTO() {{ setCono(cono); setCurrent(1); setSize(10000); }});
                    return page.getRecords().stream().map(ReportBuilderService::toMap).toList();
                }));

        // ── 采购 ──
        r.put("purchases", new DataSourceEntry("采购订单",
                List.of(
                        f("cono","公司代码","dimension"), f("pono","采购单号","dimension"),
                        f("vendor","供应商编码","dimension"), f("vendorName","供应商名称","dimension"),
                        f("orderDate","下单日期","date"), f("reqDate","需求日期","date"),
                        f("totalAmount","总金额","measure"), f("lineCount","行数","measure"),
                        f("status","状态","dimension")),
                cono -> {
                    PageResult<BpcsPurchaseOrderVO> page = purchaseService.search(new BpcsPurchaseQueryDTO() {{ setCono(cono); setCurrent(1); setSize(10000); }});
                    return page.getRecords().stream().map(ReportBuilderService::toMap).toList();
                }));

        // ── 发货 ──
        r.put("shipping", new DataSourceEntry("发货/装运",
                List.of(
                        f("cono","公司代码","dimension"), f("lhno","装运单号","dimension"),
                        f("carrier","承运商","dimension"), f("destination","目的地","dimension"),
                        f("shipDate","发货日期","date"), f("statusKey","状态","dimension"),
                        f("lineCount","行数","measure"), f("weight","重量","measure")),
                cono -> {
                    PageResult<BpcsLoadVO> page = shippingService.search(new BpcsShippingQueryDTO() {{ setCono(cono); setCurrent(1); setSize(10000); }});
                    return page.getRecords().stream().map(ReportBuilderService::toMap).toList();
                }));

        // ── 发票 ──
        r.put("invoices", new DataSourceEntry("发票",
                List.of(
                        f("invNo","发票号","dimension"), f("orno","订单号","dimension"),
                        f("cust","客户编码","dimension"), f("custName","客户名称","dimension"),
                        f("invDate","发票日期","date"), f("status","状态","dimension"),
                        f("totalAmount","总金额","measure"), f("taxAmount","税额","measure"),
                        f("lineCount","行数","measure")),
                cono -> {
                    PageResult<BpcsInvoiceVO> page = invoiceService.search(new BpcsInvoiceQueryDTO() {{ setCono(cono); setCurrent(1); setSize(10000); }});
                    return page.getRecords().stream().map(ReportBuilderService::toMap).toList();
                }));

        // ── 销售趋势 ──
        r.put("salesTrend", new DataSourceEntry("销售趋势（月度）",
                List.of(
                        f("ym","年月","date"), f("revenue","收入","measure"),
                        f("orderCount","订单数","measure"), f("lineCount","行数","measure")),
                cono -> {
                    BpcsSalesTrendVO trend = salesService.getTrend(new BpcsSalesQueryDTO() {{ setCono(cono); }});
                    return trend.months().stream().map(ReportBuilderService::toMap).toList();
                }));

        // ── 库存预警 ──
        r.put("inventoryAlert", new DataSourceEntry("库存预警（低于安全库存）",
                List.of(
                        f("item","物料编码","dimension"), f("description","物料描述","dimension"),
                        f("warehouse","仓库","dimension"), f("uom","单位","dimension"),
                        f("onHand","在库","measure"), f("allocated","已分配","measure"),
                        f("onOrder","在途","measure"), f("available","可用","measure"),
                        f("safetyStock","安全库存","measure"), f("deficit","缺口","measure")),
                cono -> supplyChainService.inventoryAlerts(cono, 500).stream().map(ReportBuilderService::toMap).toList()));

        // ── ABC 分析 ──
        r.put("abc", new DataSourceEntry("ABC 库存分析",
                List.of(
                        f("item","物料编码","dimension"), f("description","物料描述","dimension"),
                        f("warehouse","仓库","dimension"), f("quantity","数量","measure"),
                        f("unitCost","单位成本","measure"), f("stockValue","库存价值","measure"),
                        f("abcClass","ABC 分类","dimension")),
                cono -> supplyChainService.abcAnalysis(cono, 500).stream().map(ReportBuilderService::toMap).toList()));

        // ── 供应商绩效 ──
        r.put("supplier", new DataSourceEntry("供应商绩效",
                List.of(
                        f("vendorName","供应商","dimension"), f("poCount","采购单数","measure"),
                        f("onTimeCount","准时交付数","measure"), f("onTimeRate","准时率","measure"),
                        f("avgPrice","平均单价","measure")),
                cono -> supplyChainService.supplierPerformance(cono, 100).stream().map(ReportBuilderService::toMap).toList()));

        // ── KPI 汇总 ──
        r.put("kpi", new DataSourceEntry("供应链 KPI",
                List.of(
                        f("totalOrders","总订单数","measure"), f("closedOrders","已关闭","measure"),
                        f("completionRate","完成率","measure"), f("totalItems","物料数","measure"),
                        f("inventoryValue","库存价值","measure"), f("onTimeDeliveryRate","准时交付率","measure")),
                cono -> {
                    BpcsKpiVO kpi = supplyChainService.supplyChainKpi(cono);
                    return List.of(toMap(kpi));
                }));

        // ── 库存事务历史 ──
        r.put("inventoryHistory", new DataSourceEntry("库存事务历史",
                List.of(
                        f("item","物料编码","dimension"), f("warehouse","仓库","dimension"),
                        f("type","事务类型","dimension"), f("quantity","数量","measure"),
                        f("referenceNo","参考号","dimension"), f("date","日期","date"),
                        f("time","时间","dimension"), f("userId","操作人","dimension")),
                cono -> supplyChainService.inventoryHistory(cono, null, null, null, 500).stream().map(ReportBuilderService::toMap).toList()));

        // ── 采购收货 ──
        r.put("purchaseReceiving", new DataSourceEntry("采购收货状态",
                List.of(
                        f("pono","采购单号","dimension"), f("vendorName","供应商","dimension"),
                        f("orderDate","下单日期","date"), f("item","物料编码","dimension"),
                        f("itemDesc","物料描述","dimension"), f("qtyOrdered","订购数量","measure"),
                        f("qtyReceived","已收数量","measure"), f("qtyOpen","未收数量","measure"),
                        f("unitPrice","单价","measure")),
                cono -> supplyChainService.purchaseReceiving(cono, null, null, 500).stream().map(ReportBuilderService::toMap).toList()));

        // ── 销售分析（Top 客户/物料） ──
        r.put("salesAnalysis", new DataSourceEntry("销售分析（Top 客户/物料）",
                List.of(
                        f("code","编码","dimension"), f("name","名称","dimension"),
                        f("orderCount","订单数","measure"), f("totalAmount","总金额","measure")),
                cono -> {
                    BpcsSalesAnalysisVO ana = supplyChainService.salesAnalysis(cono, 10);
                    List<Map<String, Object>> list = new ArrayList<>();
                    for (var e : ana.topCustomers()) list.add(toMap(e));
                    for (var e : ana.topItems()) list.add(toMap(e));
                    return list;
                }));

        // ── 订单跟踪 ──
        r.put("orderTracking", new DataSourceEntry("订单全程跟踪",
                List.of(
                        f("cono","公司代码","dimension"), f("orno","订单号","dimension"),
                        f("custNo","客户号","dimension"), f("custName","客户名称","dimension"),
                        f("statusLabel","状态","dimension")),
                cono -> {
                    BpcsOrderTrackingVO tracking = supplyChainService.orderTracking(cono, "");
                    return tracking != null ? List.of(toMap(tracking)) : List.of();
                }));

        return r;
    }

    private static FieldMeta f(String key, String label, String type) {
        return new FieldMeta(key, label, type);
    }

    // ── 接口实现 ────────────────────────────────────────────────

    @Override
    public List<DataSourceMeta> listDataSources() {
        return buildRegistry().entrySet().stream()
                .map(e -> new DataSourceMeta(e.getKey(), e.getValue().label(), e.getValue().fields()))
                .toList();
    }

    @Override
    public List<ReportDefinitionVO> listDefinitions() {
        return definitionMapper.selectList(null).stream()
                .map(ReportDefinitionVO::from).toList();
    }

    @Override
    public ReportDefinitionVO getDefinition(Long id) {
        ReportDefinition def = definitionMapper.selectById(id);
        if (def == null) throw new BusinessException(ErrorCode.NOT_FOUND, "报表定义不存在: " + id);
        return ReportDefinitionVO.from(def);
    }

    @Override
    public ReportDefinitionVO createDefinition(ReportDefinitionDTO dto, String username) {
        ReportDefinition def = new ReportDefinition();
        def.setName(dto.getName());
        def.setDataSource(dto.getDataSource());
        def.setTitle(dto.getTitle() != null ? dto.getTitle() : "");
        def.setColumnsJson(dto.getColumnsJson());
        def.setFiltersJson(dto.getFiltersJson() != null ? dto.getFiltersJson() : "[]");
        def.setSortsJson(dto.getSortsJson() != null ? dto.getSortsJson() : "[]");
        def.setCreatedBy(username);
        definitionMapper.insert(def);
        return ReportDefinitionVO.from(def);
    }

    @Override
    public ReportDefinitionVO updateDefinition(Long id, ReportDefinitionDTO dto) {
        ReportDefinition def = definitionMapper.selectById(id);
        if (def == null) throw new BusinessException(ErrorCode.NOT_FOUND, "报表定义不存在: " + id);
        def.setName(dto.getName());
        def.setDataSource(dto.getDataSource());
        def.setTitle(dto.getTitle() != null ? dto.getTitle() : def.getTitle());
        def.setColumnsJson(dto.getColumnsJson());
        if (dto.getFiltersJson() != null) def.setFiltersJson(dto.getFiltersJson());
        if (dto.getSortsJson() != null) def.setSortsJson(dto.getSortsJson());
        definitionMapper.updateById(def);
        return ReportDefinitionVO.from(def);
    }

    @Override
    public void deleteDefinition(Long id) {
        if (definitionMapper.selectById(id) == null) throw new BusinessException(ErrorCode.NOT_FOUND, "报表定义不存在: " + id);
        definitionMapper.deleteById(id);
    }

    @Override
    public Map<String, Object> executeReport(Long id) {
        ReportDefinition def = definitionMapper.selectById(id);
        if (def == null) throw new BusinessException(ErrorCode.NOT_FOUND, "报表定义不存在: " + id);

        Map<String, DataSourceEntry> registry = buildRegistry();
        DataSourceEntry entry = registry.get(def.getDataSource());
        if (entry == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "未知数据源: " + def.getDataSource());

        // 1. 获取全量数据
        String cono = "001"; // 默认公司代码，可扩展
        List<Map<String, Object>> rawData = entry.fetcher().fetch(cono);

        // 2. 解析列定义
        List<ColumnDef> selectedCols;
        try {
            selectedCols = MAPPER.readValue(def.getColumnsJson(), COL_REF);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "列定义解析失败: " + e.getMessage());
        }
        if (selectedCols.isEmpty()) throw new BusinessException(ErrorCode.BAD_REQUEST, "请至少选择一列");

        // 3. 应用筛选
        List<FilterDef> filters = parseJson(def.getFiltersJson(), FIL_REF);

        List<Map<String, Object>> filtered = rawData.stream()
                .filter(row -> filters.stream().allMatch(f -> matchFilter(row, f)))
                .toList();

        // 4. 应用排序
        List<SortDef> sorts = parseJson(def.getSortsJson(), SOR_REF);

        List<Map<String, Object>> sorted = filtered;
        if (!sorts.isEmpty()) {
            sorted = filtered.stream().sorted((a, b) -> {
                for (SortDef s : sorts) {
                    int cmp = compareValues(a.get(s.field()), b.get(s.field()));
                    if (cmp != 0) return s.asc() ? cmp : -cmp;
                }
                return 0;
            }).toList();
        }

        // 5. 投影到选中列
        List<String> keys = selectedCols.stream().map(ColumnDef::key).toList();
        List<String> labels = selectedCols.stream().map(ColumnDef::label).toList();
        List<Map<String, Object>> projected = sorted.stream().map(row -> {
            Map<String, Object> m = new LinkedHashMap<>();
            for (String k : keys) m.put(k, row.get(k));
            return m;
        }).toList();

        // 6. 返回 { columns, rows, total, title }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("columns", labels);
        result.put("keys", keys);
        result.put("rows", projected);
        result.put("total", projected.size());
        result.put("title", def.getTitle() != null && !def.getTitle().isBlank() ? def.getTitle() : def.getName());
        result.put("dataSource", def.getDataSource());
        return result;
    }

    @Override
    public byte[] exportReport(Long id, String format) {
        Map<String, Object> data = executeReport(id);
        @SuppressWarnings("unchecked")
        List<String> columns = (List<String>) data.get("columns");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) data.get("rows");
        String title = (String) data.get("title");

        String[] headers = columns.toArray(new String[0]);
        return reportService.render(format, title, headers, rows);
    }

    // ── 筛选/比较工具 ──────────────────────────────────────────

    private boolean matchFilter(Map<String, Object> row, FilterDef f) {
        Object val = row.get(f.field());
        if (val == null) return false;
        String strVal = String.valueOf(val);
        String filterVal = f.value();
        if (filterVal == null || filterVal.isBlank()) return true;

        String op = f.op() != null ? f.op() : "contains";
        return switch (op) {
            case "eq" -> strVal.equalsIgnoreCase(filterVal);
            case "ne" -> !strVal.equalsIgnoreCase(filterVal);
            case "contains" -> strVal.toLowerCase().contains(filterVal.toLowerCase());
            case "startsWith" -> strVal.toLowerCase().startsWith(filterVal.toLowerCase());
            case "gt" -> toDouble(val) > toDouble(filterVal);
            case "gte" -> toDouble(val) >= toDouble(filterVal);
            case "lt" -> toDouble(val) < toDouble(filterVal);
            case "lte" -> toDouble(val) <= toDouble(filterVal);
            default -> true;
        };
    }

    private int compareValues(Object a, Object b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        if (a instanceof Number na && b instanceof Number nb) return Double.compare(na.doubleValue(), nb.doubleValue());
        return String.valueOf(a).compareTo(String.valueOf(b));
    }

    private double toDouble(Object o) {
        if (o instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(o)); } catch (Exception e) { return 0; }
    }

    private <T> List<T> parseJson(String json, TypeReference<List<T>> ref) {
        if (json == null || json.isBlank()) return List.of();
        try { return MAPPER.readValue(json, ref); } catch (Exception e) { return List.of(); }
    }

    // ── Record → Map 反射 ──────────────────────────────────────

    private static Map<String, Object> toMap(Object record) {
        if (record == null) return Map.of();
        Map<String, Object> map = new LinkedHashMap<>();
        for (RecordComponent c : record.getClass().getRecordComponents()) {
            try {
                var accessor = record.getClass().getDeclaredMethod(c.getName());
                Object val = accessor.invoke(record);
                if (val == null) continue;
                if (val instanceof List) continue; // 跳过嵌套 List（如 warehouses/lines）
                map.put(c.getName(), val);
            } catch (Exception e) {
                log.debug("Record component accessor failed for {}: {}", c.getName(), e.getMessage());
            }
        }
        return map;
    }

    /** BpcsSalesAnalysisVO.TopEntry 手动转 Map（非标准 Record getter） */
    private static Map<String, Object> toMap(BpcsSalesAnalysisVO.TopEntry e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", e.code());
        m.put("name", e.name());
        m.put("orderCount", e.orderCount());
        m.put("totalAmount", e.totalAmount());
        return m;
    }
}
