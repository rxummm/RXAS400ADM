package com.rxas400adm.as400;

import java.util.List;
import java.util.Map;

/**
 * 【第六章·P1】BPCS 客户订单演示数据（mock 档专用）。
 *
 * <p>三张示例订单覆盖三种典型形态，供订单时间轴页在无真机环境时完整演示：
 * <ul>
 *   <li>123456 —— 已关闭（CLSTS=11111，HID=CZ）：全流程走完；</li>
 *   <li>234567 —— 进行中（CLSTS=11000）：拣货已确认、未开票，含信用冻结演示；</li>
 *   <li>345678 —— 新录入（CLSTS=00000）：仅有订购数量。</li>
 * </ul>
 *
 * <p>字段名采用 BPCS 社区通行命名（大写），与设计文档 §2/§4.1 对应；
 * 数量字段按阶段演进填充——未到达阶段的数量键不放入行 Map，
 * 由 VO 组装的“多候选取列”自然得到 null（前端显示 “—”）。
 */
public final class MockBpcsData {

    private MockBpcsData() {
    }

    /** ECH 头演示行（单订单一条） */
    public static List<Map<String, Object>> orderHeaders() {
        return List.of(
                MockState.row(
                        "CONO", "001", "ORNO", "123456",
                        "CUST", "20315", "SHIP", "WH1",
                        "HID", "CZ", "HSTAT", "8",
                        "CHSTS1", "1", "CHSTS2", "1", "CHSTS3", "0", "CHSTS4", "0", "CHSTS5", "1",
                        "ORDTE", 20250312, "REQDTE", 20250401),
                MockState.row(
                        "CONO", "001", "ORNO", "234567",
                        "CUST", "20777", "SHIP", "WH2",
                        "HID", "CH", "HSTAT", "",
                        "CHSTS1", "1", "CHSTS2", "1", "CHSTS3", "0", "CHSTS4", "0", "CHSTS5", "0",
                        "ORDTE", 20250602, "REQDTE", 20250620),
                MockState.row(
                        "CONO", "001", "ORNO", "345678",
                        "CUST", "21001", "SHIP", "WH1",
                        "HID", "CH", "HSTAT", "E",
                        "CHSTS1", "0", "CHSTS2", "0", "CHSTS3", "0", "CHSTS4", "0", "CHSTS5", "0",
                        "ORDTE", 20250718, "REQDTE", 20250805));
    }

    /** ECL 行演示集（跨三张订单；数量键随阶段演进出现） */
    public static List<Map<String, Object>> orderLines() {
        return List.of(
                // ── 订单 123456：已关闭（全量演进）────────────────
                MockState.row(
                        "CONO", "001", "ORNO", "123456", "ORLN", "001",
                        "ITEM", "ABC-1234", "ITDSC", "液压泵总成", "WH", "WH1",
                        "QTORD", 120, "QTYALC", 120, "QTSHP", 120, "QTINV", 120,
                        "PRICE", 85.00, "DISC", 0, "REQDTE", 20250401,
                        "LID", "ZL", "LSTAT", "8",
                        "CLSTS1", "1", "CLSTS2", "1", "CLSTS3", "0", "CLSTS4", "0", "CLSTS5", "1"),
                MockState.row(
                        "CONO", "001", "ORNO", "123456", "ORLN", "002",
                        "ITEM", "ABC-1300", "ITDSC", "密封套件", "WH", "WH1",
                        "QTORD", 40, "QTYALC", 40, "QTSHP", 40, "QTINV", 40,
                        "PRICE", 12.50, "DISC", 5, "REQDTE", 20250401,
                        "LID", "ZL", "LSTAT", "8",
                        "CLSTS1", "1", "CLSTS2", "1", "CLSTS3", "0", "CLSTS4", "0", "CLSTS5", "1"),
                // ── 订单 234567：进行中（拣货确认完成，无发运/开票量）──
                MockState.row(
                        "CONO", "001", "ORNO", "234567", "ORLN", "001",
                        "ITEM", "DEF-2001", "ITDSC", "伺服电机 2kW", "WH", "WH2",
                        "QTORD", 10, "QTYALC", 10, "QTSHP", 0,
                        "PRICE", 3200.00, "DISC", 0, "REQDTE", 20250620,
                        "LSTAT", "F",
                        "CLSTS1", "1", "CLSTS2", "1", "CLSTS3", "0", "CLSTS4", "0", "CLSTS5", "0"),
                MockState.row(
                        "CONO", "001", "ORNO", "234567", "ORLN", "002",
                        "ITEM", "DEF-2010", "ITDSC", "驱动器配套线束", "WH", "WH2",
                        "QTORD", 25,
                        "PRICE", 180.00, "DISC", 3, "REQDTE", 20250625,
                        "LSTAT", "E",
                        "CLSTS1", "0", "CLSTS2", "0", "CLSTS3", "0", "CLSTS4", "0", "CLSTS5", "0"),
                // ── 订单 345678：新录入（仅订购量）────────────────
                MockState.row(
                        "CONO", "001", "ORNO", "345678", "ORLN", "001",
                        "ITEM", "GHI-3005", "ITDSC", "精密齿轮", "WH", "WH1",
                        "QTORD", 200,
                        "PRICE", 46.80, "DISC", 0, "REQDTE", 20250805,
                        "LSTAT", "E",
                        "CLSTS1", "0", "CLSTS2", "0", "CLSTS3", "0", "CLSTS4", "0", "CLSTS5", "0"));
    }

    // ---- P2: 客户档案（RCM + EST） ----

    /** RCM 客户主档演示行 */
    public static List<Map<String, Object>> customerMasters() {
        return List.of(
                MockState.row(
                        "CONO", "001", "CUST", "20315", "CUNAME", "上海精工机械有限公司",
                        "ADDR1", "浦东新区张江高科技园区", "ADDR2", "碧波路888号",
                        "CITY", "上海", "STATE", "SH", "ZIP", "201203",
                        "PHONE", "021-58889999", "CONTACT", "张经理",
                        "CRLMT", 500000.00, "TERMS", "N30", "TAX", "A1", "SAREA", "EAST"),
                MockState.row(
                        "CONO", "001", "CUST", "20777", "CUNAME", "江苏恒达传动设备厂",
                        "ADDR1", "常州市新北区", "ADDR2", "太湖东路100号",
                        "CITY", "常州", "STATE", "JS", "ZIP", "213000",
                        "PHONE", "0519-88887777", "CONTACT", "李工",
                        "CRLMT", 300000.00, "TERMS", "N60", "TAX", "A1", "SAREA", "EAST"),
                MockState.row(
                        "CONO", "001", "CUST", "21001", "CUNAME", "浙江力源液压科技",
                        "ADDR1", "杭州市余杭区仓前街道", "ADDR2", "文一西路123号",
                        "CITY", "杭州", "STATE", "ZJ", "ZIP", "311100",
                        "PHONE", "0571-88665544", "CONTACT", "王总",
                        "CRLMT", 800000.00, "TERMS", "N30", "TAX", "A2", "SAREA", "EAST"));
    }

    /** EST 收货点演示行 */
    public static List<Map<String, Object>> customerShipTos() {
        return List.of(
                MockState.row(
                        "CONO", "001", "CUST", "20315", "SHIP", "WH1",
                        "SHNAME", "华东总仓", "SHADDR1", "上海市浦东新区金桥镇",
                        "SHCITY", "上海", "SHSTATE", "SH", "SHZIP", "201206",
                        "SHPHONE", "021-58889901"),
                MockState.row(
                        "CONO", "001", "CUST", "20315", "SHIP", "WH3",
                        "SHNAME", "昆山分仓", "SHADDR1", "昆山市开发区长江路",
                        "SHCITY", "昆山", "SHSTATE", "JS", "SHZIP", "215300",
                        "SHPHONE", "0512-57881234"),
                MockState.row(
                        "CONO", "001", "CUST", "20777", "SHIP", "WH2",
                        "SHNAME", "常州直发点", "SHADDR1", "常州市新北区黄河路",
                        "SHCITY", "常州", "SHSTATE", "JS", "SHZIP", "213000",
                        "SHPHONE", "0519-88887700"),
                MockState.row(
                        "CONO", "001", "CUST", "21001", "SHIP", "WH1",
                        "SHNAME", "杭州办公室", "SHADDR1", "杭州市余杭区文一西路",
                        "SHCITY", "杭州", "SHSTATE", "ZJ", "SHZIP", "311100",
                        "SHPHONE", "0571-88665500"));
    }

    // ---- P2: 库存可用量（IIM + IWI） ----

    /** IIM 物料主档 + IWI 仓库库存 演示行 */
    public static List<Map<String, Object>> inventoryItems() {
        return List.of(
                MockState.row(
                        "CONO", "001", "ITEM", "ABC-1234", "ITDSC", "液压泵总成",
                        "UOM", "EA", "WH", "WH1", "IOHB", 200, "IISSU", 80,
                        "IRCT", 50, "IADJU", 0, "ICOST", 85.00, "LOC", "A-01-02"),
                MockState.row(
                        "CONO", "001", "ITEM", "ABC-1234", "ITDSC", "液压泵总成",
                        "UOM", "EA", "WH", "WH2", "IOHB", 120, "IISSU", 30,
                        "IRCT", 20, "IADJU", 0, "ICOST", 85.00, "LOC", "B-03-01"),
                MockState.row(
                        "CONO", "001", "ITEM", "ABC-1300", "ITDSC", "密封套件",
                        "UOM", "SET", "WH", "WH1", "IOHB", 500, "IISSU", 120,
                        "IRCT", 0, "IADJU", 0, "ICOST", 12.50, "LOC", "A-02-05"),
                MockState.row(
                        "CONO", "001", "ITEM", "DEF-2001", "ITDSC", "伺服电机 2kW",
                        "UOM", "EA", "WH", "WH2", "IOHB", 15, "IISSU", 10,
                        "IRCT", 5, "IADJU", 0, "ICOST", 3200.00, "LOC", "C-01-01"),
                MockState.row(
                        "CONO", "001", "ITEM", "GHI-3005", "ITDSC", "精密齿轮",
                        "UOM", "EA", "WH", "WH1", "IOHB", 800, "IISSU", 200,
                        "IRCT", 100, "IADJU", -5, "ICOST", 46.80, "LOC", "D-05-03"));
    }

    // ---- P2: 发运/载荷看板（LLH + LLM） ----

    /** LLH 载荷头演示行 */
    public static List<Map<String, Object>> loadHeaders() {
        return List.of(
                MockState.row(
                        "CONO", "001", "LHNO", "L-20250701", "LHSTAT", 0,
                        "CARRIER", "", "DEST", "", "SHIPDTE", 20250715,
                        "ORDNOS", "345678", "LINECT", 3, "WEIGHT", 120.5),
                MockState.row(
                        "CONO", "001", "LHNO", "L-20250615", "LHSTAT", 1,
                        "CARRIER", "德邦物流", "DEST", "上海浦东", "SHIPDTE", 20250620,
                        "ORDNOS", "234567", "LINECT", 2, "WEIGHT", 85.0),
                MockState.row(
                        "CONO", "001", "LHNO", "L-20250601", "LHSTAT", 2,
                        "CARRIER", "顺丰速运", "DEST", "常州新北", "SHIPDTE", 20250605,
                        "ORDNOS", "123456", "LINECT", 2, "WEIGHT", 200.0),
                MockState.row(
                        "CONO", "001", "LHNO", "L-20250520", "LHSTAT", 3,
                        "CARRIER", "中通快递", "DEST", "杭州余杭", "SHIPDTE", 20250522,
                        "ORDNOS", "123456", "LINECT", 2, "WEIGHT", 150.0),
                MockState.row(
                        "CONO", "001", "LHNO", "L-20250510", "LHSTAT", 3,
                        "CARRIER", "德邦物流", "DEST", "上海金桥", "SHIPDTE", 20250512,
                        "ORDNOS", "234567", "LINECT", 1, "WEIGHT", 60.0));
    }

    // ---- P2: 发票轨迹（SIH/SIL 历史 + BBH/BBL 在制） ----

    /** 在制发票（BBH/BBL）演示行 */
    public static List<Map<String, Object>> activeInvoices() {
        return List.of(
                MockState.row(
                        "INVNO", "INV-A001", "ORNO", "345678", "CUST", "21001",
                        "CUNAME", "浙江力源液压科技", "INVDATE", 20250720,
                        "STATUS", "active", "TOTAL", 9360.00, "TAX", 1216.80, "LINECT", 1,
                        "LNO", "1", "ITEM", "GHI-3005", "ITDSC", "精密齿轮",
                        "QTY", 200, "UPRICE", 46.80, "LAMT", 9360.00));
    }

    /** 历史发票（SIH/SIL）演示行 */
    public static List<Map<String, Object>> historicalInvoices() {
        return List.of(
                MockState.row(
                        "INVNO", "INV-H001", "ORNO", "123456", "CUST", "20315",
                        "CUNAME", "上海精工机械有限公司", "INVDATE", 20250320,
                        "STATUS", "posted", "TOTAL", 12450.00, "TAX", 1618.50, "LINECT", 2,
                        "LNO", "1", "ITEM", "ABC-1234", "ITDSC", "液压泵总成",
                        "QTY", 120, "UPRICE", 85.00, "LAMT", 10200.00),
                MockState.row(
                        "INVNO", "INV-H001", "ORNO", "123456", "CUST", "20315",
                        "CUNAME", "上海精工机械有限公司", "INVDATE", 20250320,
                        "STATUS", "posted", "TOTAL", 12450.00, "TAX", 1618.50, "LINECT", 2,
                        "LNO", "2", "ITEM", "ABC-1300", "ITDSC", "密封套件",
                        "QTY", 40, "UPRICE", 12.50, "LAMT", 500.00),
                MockState.row(
                        "INVNO", "INV-H002", "ORNO", "234567", "CUST", "20777",
                        "CUNAME", "江苏恒达传动设备厂", "INVDATE", 20250615,
                        "STATUS", "posted", "TOTAL", 32000.00, "TAX", 4160.00, "LINECT", 1,
                        "LNO", "1", "ITEM", "DEF-2001", "ITDSC", "伺服电机 2kW",
                        "QTY", 10, "UPRICE", 3200.00, "LAMT", 32000.00),
                MockState.row(
                        "INVNO", "INV-H003", "ORNO", "123456", "CUST", "20315",
                        "CUNAME", "上海精工机械有限公司", "INVDATE", 20250210,
                        "STATUS", "cancelled", "TOTAL", 5100.00, "TAX", 663.00, "LINECT", 1,
                        "LNO", "1", "ITEM", "ABC-1234", "ITDSC", "液压泵总成",
                        "QTY", 60, "UPRICE", 85.00, "LAMT", 5100.00));
    }

    // ---- P2: 销售趋势（SSH/SSD 月度聚合） ----

    /** 销售月度数据 */
    public static List<Map<String, Object>> salesMonthly() {
        return List.of(
                MockState.row("YM", "2025-01", "REVENUE", 85600.00, "ORDERS", 12, "LINES", 38),
                MockState.row("YM", "2025-02", "REVENUE", 92300.00, "ORDERS", 15, "LINES", 45),
                MockState.row("YM", "2025-03", "REVENUE", 124500.00, "ORDERS", 18, "LINES", 56),
                MockState.row("YM", "2025-04", "REVENUE", 108200.00, "ORDERS", 14, "LINES", 42),
                MockState.row("YM", "2025-05", "REVENUE", 135800.00, "ORDERS", 20, "LINES", 62),
                MockState.row("YM", "2025-06", "REVENUE", 142600.00, "ORDERS", 22, "LINES", 68),
                MockState.row("YM", "2025-07", "REVENUE", 98400.00, "ORDERS", 16, "LINES", 48));
    }

    // ---- P2: 采购订单（HPH + HPO） ----

    /** 采购订单头演示行 */
    public static List<Map<String, Object>> purchaseOrders() {
        return List.of(
                MockState.row(
                        "CONO", "001", "PONO", "PO-20250601", "VENDOR", "V1001",
                        "VNAME", "苏州精密轴承厂", "PODATE", 20250601,
                        "REQDTE", 20250701, "TOTAL", 45600.00, "LINECT", 3, "STATUS", 0,
                        "LNO", "1", "ITEM", "BEA-5001", "ITDSC", "深沟球轴承 6205",
                        "QTYORD", 500, "QTYRCV", 0, "UPRICE", 18.00, "LREQDTE", 20250701),
                MockState.row(
                        "CONO", "001", "PONO", "PO-20250601", "VENDOR", "V1001",
                        "VNAME", "苏州精密轴承厂", "PODATE", 20250601,
                        "REQDTE", 20250701, "TOTAL", 45600.00, "LINECT", 3, "STATUS", 0,
                        "LNO", "2", "ITEM", "BEA-5002", "ITDSC", "角接触球轴承 7206",
                        "QTYORD", 200, "QTYRCV", 0, "UPRICE", 45.00, "LREQDTE", 20250701),
                MockState.row(
                        "CONO", "001", "PONO", "PO-20250601", "VENDOR", "V1001",
                        "VNAME", "苏州精密轴承厂", "PODATE", 20250601,
                        "REQDTE", 20250701, "TOTAL", 45600.00, "LINECT", 3, "STATUS", 0,
                        "LNO", "3", "ITEM", "SEAL-200", "ITDSC", "O 型密封圈套装",
                        "QTYORD", 1000, "QTYRCV", 0, "UPRICE", 6.60, "LREQDTE", 20250620),
                MockState.row(
                        "CONO", "001", "PONO", "PO-20250515", "VENDOR", "V2002",
                        "VNAME", "无锡特种钢材公司", "PODATE", 20250515,
                        "REQDTE", 20250615, "TOTAL", 28800.00, "LINECT", 2, "STATUS", 2,
                        "LNO", "1", "ITEM", "STL-40Cr", "ITDSC", "40Cr 合金圆钢 φ50",
                        "QTYORD", 30, "QTYRCV", 30, "UPRICE", 960.00, "LREQDTE", 20250615),
                MockState.row(
                        "CONO", "001", "PONO", "PO-20250515", "VENDOR", "V2002",
                        "VNAME", "无锡特种钢材公司", "PODATE", 20250515,
                        "REQDTE", 20250615, "TOTAL", 28800.00, "LINECT", 2, "STATUS", 2,
                        "LNO", "2", "ITEM", "STL-35CrMo", "ITDSC", "35CrMo 合金圆钢 φ40",
                        "QTYORD", 20, "QTYRCV", 20, "UPRICE", 720.00, "LREQDTE", 20250615),
                MockState.row(
                        "CONO", "001", "PONO", "PO-20250420", "VENDOR", "V3003",
                        "VNAME", "东莞液压元件有限公司", "PODATE", 20250420,
                        "REQDTE", 20250520, "TOTAL", 15200.00, "LINECT", 1, "STATUS", 3,
                        "LNO", "1", "ITEM", "HYP-300", "ITDSC", "液压缸总成 HG-63",
                        "QTYORD", 10, "QTYRCV", 10, "UPRICE", 1520.00, "LREQDTE", 20250520));
    }

    // ---- P2: 物料主档（IIM + IWI 多维度） ----

    /** IIM 物料主档基础信息 */
    public static List<Map<String, Object>> itemMasters() {
        return List.of(
                MockState.row(
                        "ITEM", "ABC-1234", "ITDSC", "液压泵总成",
                        "UOM", "EA", "ICAT", "液压件", "ICOST", 85.00,
                        "ILPRT", 120.00, "IWEIGHT", 12.5, "ISHLF", 365),
                MockState.row(
                        "ITEM", "ABC-1300", "ITDSC", "密封套件",
                        "UOM", "SET", "ICAT", "密封件", "ICOST", 12.50,
                        "ILPRT", 18.00, "IWEIGHT", 0.8, "ISHLF", 730),
                MockState.row(
                        "ITEM", "DEF-2001", "ITDSC", "伺服电机 2kW",
                        "UOM", "EA", "ICAT", "电机", "ICOST", 3200.00,
                        "ILPRT", 4500.00, "IWEIGHT", 28.0, "ISHLF", null),
                MockState.row(
                        "ITEM", "GHI-3005", "ITDSC", "精密齿轮",
                        "UOM", "EA", "ICAT", "传动件", "ICOST", 46.80,
                        "ILPRT", 68.00, "IWEIGHT", 3.2, "ISHLF", null),
                MockState.row(
                        "ITEM", "BEA-5001", "ITDSC", "深沟球轴承 6205",
                        "UOM", "EA", "ICAT", "轴承", "ICOST", 18.00,
                        "ILPRT", 28.00, "IWEIGHT", 0.25, "ISHLF", null));
    }

    /** 物料最近采购记录 */
    public static List<Map<String, Object>> itemPurchases() {
        return List.of(
                MockState.row("ITEM", "ABC-1234", "PONO", "PO-20250601", "VNAME", "苏州精密轴承厂",
                        "ODATE", 20250601, "QTY", 200, "UPRICE", 80.00),
                MockState.row("ITEM", "ABC-1234", "PONO", "PO-20250415", "VNAME", "苏州精密轴承厂",
                        "ODATE", 20250415, "QTY", 150, "UPRICE", 82.00),
                MockState.row("ITEM", "DEF-2001", "PONO", "PO-20250515", "VNAME", "无锡特种钢材公司",
                        "ODATE", 20250515, "QTY", 10, "UPRICE", 3100.00),
                MockState.row("ITEM", "GHI-3005", "PONO", "PO-20250601", "VNAME", "苏州精密轴承厂",
                        "ODATE", 20250601, "QTY", 500, "UPRICE", 44.00));
    }

    /** 物料最近销售记录 */
    public static List<Map<String, Object>> itemSales() {
        return List.of(
                MockState.row("ITEM", "ABC-1234", "ORNO", "123456", "CNAME", "上海精工机械有限公司",
                        "ODATE", 20250312, "QTY", 120, "UPRICE", 85.00),
                MockState.row("ITEM", "ABC-1300", "ORNO", "123456", "CNAME", "上海精工机械有限公司",
                        "ODATE", 20250312, "QTY", 40, "UPRICE", 12.50),
                MockState.row("ITEM", "DEF-2001", "ORNO", "234567", "CNAME", "江苏恒达传动设备厂",
                        "ODATE", 20250602, "QTY", 10, "UPRICE", 3200.00),
                MockState.row("ITEM", "GHI-3005", "ORNO", "345678", "CNAME", "浙江力源液压科技",
                        "ODATE", 20250718, "QTY", 200, "UPRICE", 46.80));
    }
}