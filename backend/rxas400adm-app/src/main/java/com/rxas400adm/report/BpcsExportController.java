package com.rxas400adm.report;

import com.rxas400adm.as400.service.*;
import com.rxas400adm.common.response.PageResult;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * BPCS 全页面导出：统一处理 Excel / PDF 导出。
 * 前端 CSV 由 ExportDropdown 组件客户端生成；Excel / PDF 走本端点。
 * 复用 ReportRenderer（POI Excel + OpenPDF PDF）。
 */
@RestController
@RequestMapping("/api/v1/bpcs/export")
@RequiredArgsConstructor
@Tag(name = "BPCS导出")
public class BpcsExportController {

    private final IReportService reportService;
    private final IBpcsOrderService orderService;
    private final IBpcsCustomerService customerService;
    private final IBpcsInventoryService inventoryService;
    private final IBpcsShippingService shippingService;
    private final IBpcsInvoiceService invoiceService;
    private final IBpcsSalesService salesService;
    private final IBpcsPurchaseService purchaseService;
    private final IBpcsItemService itemService;
    private final IBpcsSupplyChainService supplyChainService;

    @GetMapping("/orders")
    @PreAuthorize("hasAuthority('BPCS_ORDER_VIEW')")
    public byte[] orders(@RequestParam(defaultValue = "xlsx") String format,
                         @RequestParam String cono,
                         @RequestParam String orno) {
        var query = new com.rxas400adm.as400.dto.BpcsOrderQueryDTO();
        query.setCono(cono);
        query.setOrno(orno);
        var vo = orderService.getHeader(query);
        List<Map<String, Object>> rows = orderRows(vo);
        return reportService.render(format, "BPCS订单", orderHeaders(), rows);
    }

    @GetMapping("/customers")
    @PreAuthorize("hasAuthority('BPCS_CUSTOMER_VIEW')")
    public byte[] customers(@RequestParam(defaultValue = "xlsx") String format,
                            @RequestParam(required = false) String cono,
                            @RequestParam(required = false) String cust,
                            @RequestParam(required = false) String name) {
        var query = new com.rxas400adm.as400.dto.BpcsCustomerQueryDTO();
        query.setCono(cono);
        query.setCust(cust);
        query.setName(name);
        query.setCurrent(1);
        query.setSize(10000);
        PageResult<?> result = customerService.search(query);
        List<Map<String, Object>> rows = customerRows(result.getRecords());
        return reportService.render(format, "BPCS客户列表", customerHeaders(), rows);
    }

    @GetMapping("/inventory")
    @PreAuthorize("hasAuthority('BPCS_INVENTORY_VIEW')")
    public byte[] inventory(@RequestParam(defaultValue = "xlsx") String format,
                            @RequestParam(required = false) String cono,
                            @RequestParam(required = false) String item,
                            @RequestParam(required = false) String desc,
                            @RequestParam(required = false) String wh) {
        var query = new com.rxas400adm.as400.dto.BpcsInventoryQueryDTO();
        query.setCono(cono);
        query.setItem(item);
        query.setDesc(desc);
        query.setWh(wh);
        query.setCurrent(1);
        query.setSize(10000);
        PageResult<?> result = inventoryService.search(query);
        List<Map<String, Object>> rows = inventoryRows(result.getRecords());
        return reportService.render(format, "BPCS库存列表", inventoryHeaders(), rows);
    }

    @GetMapping("/shipping")
    @PreAuthorize("hasAuthority('BPCS_SHIPPING_VIEW')")
    public byte[] shipping(@RequestParam(defaultValue = "xlsx") String format,
                           @RequestParam(required = false) String cono,
                           @RequestParam(required = false) String lhno) {
        var query = new com.rxas400adm.as400.dto.BpcsShippingQueryDTO();
        query.setCono(cono);
        query.setLhno(lhno);
        query.setCurrent(1);
        query.setSize(10000);
        PageResult<?> result = shippingService.search(query);
        List<Map<String, Object>> rows = shippingRows(result.getRecords());
        return reportService.render(format, "BPCS发运列表", shippingHeaders(), rows);
    }

    @GetMapping("/invoices")
    @PreAuthorize("hasAuthority('BPCS_INVOICE_VIEW')")
    public byte[] invoices(@RequestParam(defaultValue = "xlsx") String format,
                           @RequestParam(required = false) String cono,
                           @RequestParam(required = false) String orno) {
        var query = new com.rxas400adm.as400.dto.BpcsInvoiceQueryDTO();
        query.setCono(cono);
        query.setOrno(orno);
        query.setCurrent(1);
        query.setSize(10000);
        PageResult<?> result = invoiceService.search(query);
        List<Map<String, Object>> rows = invoiceRows(result.getRecords());
        return reportService.render(format, "BPCS发票列表", invoiceHeaders(), rows);
    }

    @GetMapping("/sales")
    @PreAuthorize("hasAuthority('BPCS_SALES_VIEW')")
    public byte[] sales(@RequestParam(defaultValue = "xlsx") String format,
                        @RequestParam(required = false) String cono,
                        @RequestParam(required = false) String fromYm,
                        @RequestParam(required = false) String toYm) {
        var query = new com.rxas400adm.as400.dto.BpcsSalesQueryDTO();
        query.setCono(cono);
        query.setFromYm(fromYm);
        query.setToYm(toYm);
        var trend = salesService.getTrend(query);
        List<Map<String, Object>> rows = salesRows(trend);
        return reportService.render(format, "BPCS销售趋势", salesHeaders(), rows);
    }

    @GetMapping("/purchases")
    @PreAuthorize("hasAuthority('BPCS_PURCHASE_VIEW')")
    public byte[] purchases(@RequestParam(defaultValue = "xlsx") String format,
                            @RequestParam(required = false) String cono,
                            @RequestParam(required = false) String pono,
                            @RequestParam(required = false) String vendor) {
        var query = new com.rxas400adm.as400.dto.BpcsPurchaseQueryDTO();
        query.setCono(cono);
        query.setPono(pono);
        query.setVendor(vendor);
        query.setCurrent(1);
        query.setSize(10000);
        PageResult<?> result = purchaseService.search(query);
        List<Map<String, Object>> rows = purchaseRows(result.getRecords());
        return reportService.render(format, "BPCS采购列表", purchaseHeaders(), rows);
    }

    @GetMapping("/items")
    @PreAuthorize("hasAuthority('BPCS_ITEM_VIEW')")
    public byte[] items(@RequestParam(defaultValue = "xlsx") String format,
                        @RequestParam(required = false) String cono,
                        @RequestParam(required = false) String item,
                        @RequestParam(required = false) String desc) {
        var query = new com.rxas400adm.as400.dto.BpcsItemQueryDTO();
        query.setCono(cono);
        query.setItem(item);
        query.setDesc(desc);
        query.setCurrent(1);
        query.setSize(10000);
        PageResult<?> result = itemService.search(query);
        List<Map<String, Object>> rows = itemRows(result.getRecords());
        return reportService.render(format, "BPCS物料列表", itemHeaders(), rows);
    }

    @GetMapping("/supply-chain/{type}")
    @PreAuthorize("hasAuthority('BPCS_ORDER_VIEW')")
    public byte[] supplyChain(@RequestParam(defaultValue = "xlsx") String format,
                              @PathVariable String type,
                              @RequestParam(required = false) String cono,
                              @RequestParam(required = false) String item,
                              @RequestParam(required = false) String fromDate,
                              @RequestParam(required = false) String toDate,
                              @RequestParam(required = false) String pono,
                              @RequestParam(required = false) String vendor,
                              @RequestParam(defaultValue = "100") int limit) {
        String title;
        String[] headers;
        List<Map<String, Object>> rows;
        switch (type) {
            case "orders" -> {
                var q = new com.rxas400adm.as400.dto.BpcsOrderListQueryDTO();
                q.setCono(cono);
                rows = scOrderRows(supplyChainService.searchOrders(q));
                title = "BPCS供应链订单";
                headers = scOrderHeaders();
            }
            case "alerts" -> {
                rows = scAlertRows(supplyChainService.inventoryAlerts(cono, limit));
                title = "BPCS库存预警";
                headers = scAlertHeaders();
            }
            case "analysis" -> {
                var vo = supplyChainService.salesAnalysis(cono, limit);
                rows = scAnalysisRows(vo);
                title = "BPCS销售分析";
                headers = scAnalysisHeaders();
            }
            case "history" -> {
                rows = scHistoryRows(supplyChainService.inventoryHistory(cono, item, fromDate, toDate, limit));
                title = "BPCS库存历史";
                headers = scHistoryHeaders();
            }
            case "receiving" -> {
                rows = scReceivingRows(supplyChainService.purchaseReceiving(cono, pono, vendor, limit));
                title = "BPCS采购收货";
                headers = scReceivingHeaders();
            }
            case "abc" -> {
                rows = scAbcRows(supplyChainService.abcAnalysis(cono, limit));
                title = "BPCS ABC分析";
                headers = scAbcHeaders();
            }
            case "supplier" -> {
                rows = scSupplierRows(supplyChainService.supplierPerformance(cono, limit));
                title = "BPCS供应商绩效";
                headers = scSupplierHeaders();
            }
            default -> {
                return new byte[0];
            }
        }
        return reportService.render(format, title, headers, rows);
    }

    // ==================== Row conversion (record accessors: field() not get_FIELD()) ====================

    private static List<Map<String, Object>> orderRows(com.rxas400adm.as400.vo.BpcsOrderHeaderVO v) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (v == null) return list;
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("公司", v.cono());
        map.put("订单号", v.orno());
        map.put("客户号", v.customerNo());
        map.put("收货点", v.shipTo());
        map.put("订单日期", v.orderDate());
        map.put("要求日期", v.reqDate());
        map.put("订单金额", v.totalAmount());
        map.put("行数", v.lineCount());
        list.add(map);
        return list;
    }

    private static String[] orderHeaders() {
        return new String[]{"公司", "订单号", "客户号", "收货点", "订单日期", "要求日期", "订单金额", "行数"};
    }

    private static List<Map<String, Object>> customerRows(List<?> records) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object rec : records) {
            @SuppressWarnings("unchecked")
            var v = (com.rxas400adm.as400.vo.BpcsCustomerVO) rec;
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("公司", v.cono());
            map.put("客户号", v.cust());
            map.put("客户名称", v.name());
            map.put("地址", v.address1());
            map.put("城市", v.city());
            map.put("州", v.state());
            map.put("邮编", v.zip());
            map.put("电话", v.phone());
            map.put("联系人", v.contact());
            map.put("信用额度", v.creditLimit());
            list.add(map);
        }
        return list;
    }

    private static String[] customerHeaders() {
        return new String[]{"公司", "客户号", "客户名称", "地址", "城市", "州", "邮编", "电话", "联系人", "信用额度"};
    }

    private static List<Map<String, Object>> inventoryRows(List<?> records) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object rec : records) {
            @SuppressWarnings("unchecked")
            var v = (com.rxas400adm.as400.vo.BpcsInventoryVO) rec;
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("物料号", v.item());
            map.put("描述", v.description());
            map.put("单位", v.uom());
            map.put("在手量", v.totalOnHand());
            map.put("已分配", v.totalAllocated());
            map.put("在订量", v.totalOnOrder());
            map.put("可用量", v.totalAvailable());
            map.put("单位成本", v.unitCost());
            list.add(map);
        }
        return list;
    }

    private static String[] inventoryHeaders() {
        return new String[]{"物料号", "描述", "单位", "在手量", "已分配", "在订量", "可用量", "单位成本"};
    }

    private static List<Map<String, Object>> shippingRows(List<?> records) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object rec : records) {
            @SuppressWarnings("unchecked")
            var v = (com.rxas400adm.as400.vo.BpcsLoadVO) rec;
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("公司", v.cono());
            map.put("载荷号", v.lhno());
            map.put("状态", v.statusKey());
            map.put("承运商", v.carrier());
            map.put("目的地", v.destination());
            map.put("发运日期", v.shipDate());
            map.put("订单数", v.orderNos() != null ? v.orderNos().size() : 0);
            map.put("重量", v.weight());
            list.add(map);
        }
        return list;
    }

    private static String[] shippingHeaders() {
        return new String[]{"公司", "载荷号", "状态", "承运商", "目的地", "发运日期", "订单数", "重量"};
    }

    private static List<Map<String, Object>> invoiceRows(List<?> records) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object rec : records) {
            @SuppressWarnings("unchecked")
            var v = (com.rxas400adm.as400.vo.BpcsInvoiceVO) rec;
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("发票号", v.invNo());
            map.put("订单号", v.orno());
            map.put("客户号", v.cust());
            map.put("客户名称", v.custName());
            map.put("发票日期", v.invDate());
            map.put("状态", v.statusKey());
            map.put("总金额", v.totalAmount());
            map.put("税额", v.taxAmount());
            list.add(map);
        }
        return list;
    }

    private static String[] invoiceHeaders() {
        return new String[]{"发票号", "订单号", "客户号", "客户名称", "发票日期", "状态", "总金额", "税额"};
    }

    private static List<Map<String, Object>> salesRows(Object trendVo) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (trendVo == null) return list;
        @SuppressWarnings("unchecked")
        var trend = (com.rxas400adm.as400.vo.BpcsSalesTrendVO) trendVo;
        if (trend.months() != null) {
            for (var m : trend.months()) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("年月", m.ym());
                map.put("收入", m.revenue());
                map.put("订单数", m.orderCount());
                map.put("行数", m.lineCount());
                list.add(map);
            }
        }
        return list;
    }

    private static String[] salesHeaders() {
        return new String[]{"年月", "收入", "订单数", "行数"};
    }

    private static List<Map<String, Object>> purchaseRows(List<?> records) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object rec : records) {
            @SuppressWarnings("unchecked")
            var v = (com.rxas400adm.as400.vo.BpcsPurchaseOrderVO) rec;
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("公司", v.cono());
            map.put("采购单号", v.pono());
            map.put("供应商", v.vendor());
            map.put("供应商名称", v.vendorName());
            map.put("订单日期", v.orderDate());
            map.put("要求日期", v.reqDate());
            map.put("总金额", v.totalAmount());
            map.put("行数", v.lineCount());
            map.put("状态", v.statusKey());
            list.add(map);
        }
        return list;
    }

    private static String[] purchaseHeaders() {
        return new String[]{"公司", "采购单号", "供应商", "供应商名称", "订单日期", "要求日期", "总金额", "行数", "状态"};
    }

    private static List<Map<String, Object>> itemRows(List<?> records) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object rec : records) {
            @SuppressWarnings("unchecked")
            var v = (com.rxas400adm.as400.vo.BpcsItemVO) rec;
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("物料号", v.item());
            map.put("描述", v.description());
            map.put("单位", v.uom());
            map.put("分类", v.category());
            map.put("单位成本", v.unitCost());
            map.put("列表价", v.listPrice());
            map.put("在手量", v.totalOnHand());
            map.put("已分配", v.totalAllocated());
            map.put("可用量", v.totalAvailable());
            list.add(map);
        }
        return list;
    }

    private static String[] itemHeaders() {
        return new String[]{"物料号", "描述", "单位", "分类", "单位成本", "列表价", "在手量", "已分配", "可用量"};
    }

    // ==================== Supply-chain row helpers ====================

    private static List<Map<String, Object>> scOrderRows(List<com.rxas400adm.as400.vo.BpcsOrderListVO> list) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (var v : list) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("公司", v.cono());
            map.put("订单号", v.orno());
            map.put("客户号", v.custNo());
            map.put("客户名称", v.custName());
            map.put("订单日期", v.orderDate());
            map.put("状态", v.statusLabel());
            map.put("行数", v.lineCount());
            rows.add(map);
        }
        return rows;
    }

    private static String[] scOrderHeaders() {
        return new String[]{"公司", "订单号", "客户号", "客户名称", "订单日期", "状态", "总金额"};
    }

    private static List<Map<String, Object>> scAlertRows(List<com.rxas400adm.as400.vo.BpcsInventoryAlertVO> list) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (var v : list) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("物料号", v.item());
            map.put("描述", v.description());
            map.put("仓库", v.warehouse());
            map.put("在手量", v.onHand());
            map.put("安全库存", v.safetyStock());
            map.put("缺口", v.deficit());
            rows.add(map);
        }
        return rows;
    }

    private static String[] scAlertHeaders() {
        return new String[]{"物料号", "描述", "仓库", "在手量", "安全库存", "缺口"};
    }

    private static List<Map<String, Object>> scAnalysisRows(com.rxas400adm.as400.vo.BpcsSalesAnalysisVO vo) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (vo == null || vo.topItems() == null) return rows;
        for (var v : vo.topItems()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("编码", v.code());
            map.put("名称", v.name());
            map.put("收入", v.totalAmount());
            map.put("订单数", v.orderCount());
            rows.add(map);
        }
        return rows;
    }

    private static String[] scAnalysisHeaders() {
        return new String[]{"编码", "名称", "收入", "订单数"};
    }

    private static List<Map<String, Object>> scHistoryRows(List<com.rxas400adm.as400.vo.BpcsInventoryHistoryVO> list) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (var v : list) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("物料号", v.item());
            map.put("仓库", v.warehouse());
            map.put("事务类型", v.type());
            map.put("数量", v.quantity());
            map.put("参考号", v.referenceNo());
            map.put("日期", v.date());
            rows.add(map);
        }
        return rows;
    }

    private static String[] scHistoryHeaders() {
        return new String[]{"物料号", "仓库", "事务类型", "数量", "参考号", "日期"};
    }

    private static List<Map<String, Object>> scReceivingRows(List<com.rxas400adm.as400.vo.BpcsPurchaseReceivingVO> list) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (var v : list) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("采购单号", v.pono());
            map.put("供应商名称", v.vendorName());
            map.put("物料号", v.item());
            map.put("物料描述", v.itemDesc());
            map.put("已订购", v.qtyOrdered());
            map.put("已收货", v.qtyReceived());
            map.put("未结量", v.qtyOpen());
            map.put("单价", v.unitPrice());
            rows.add(map);
        }
        return rows;
    }

    private static String[] scReceivingHeaders() {
        return new String[]{"采购单号", "供应商名称", "物料号", "物料描述", "已订购", "已收货", "未结量", "单价"};
    }

    private static List<Map<String, Object>> scAbcRows(List<com.rxas400adm.as400.vo.BpcsAbcAnalysisVO> list) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (var v : list) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("物料号", v.item());
            map.put("描述", v.description());
            map.put("仓库", v.warehouse());
            map.put("数量", v.quantity());
            map.put("单位成本", v.unitCost());
            map.put("库存价值", v.stockValue());
            map.put("分类", v.abcClass());
            rows.add(map);
        }
        return rows;
    }

    private static String[] scAbcHeaders() {
        return new String[]{"物料号", "描述", "仓库", "数量", "单位成本", "库存价值", "分类"};
    }

    private static List<Map<String, Object>> scSupplierRows(List<com.rxas400adm.as400.vo.BpcsSupplierPerfVO> list) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (var v : list) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("供应商名称", v.vendorName());
            map.put("采购单数", v.poCount());
            map.put("准时交付数", v.onTimeCount());
            map.put("准时交付率%", v.onTimeRate());
            map.put("平均单价", v.avgPrice());
            rows.add(map);
        }
        return rows;
    }

    private static String[] scSupplierHeaders() {
        return new String[]{"供应商名称", "采购单数", "准时交付数", "准时交付率%", "平均单价"};
    }
}
