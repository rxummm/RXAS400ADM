package com.rxas400adm.dashboard;

import com.rxas400adm.as400.AS400Client;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.entity.IbmiSystem;
import com.rxas400adm.as400.service.IIbmiSystemService;
import com.rxas400adm.as400.vo.ExecutiveSummaryVO;
import com.rxas400adm.as400.vo.ExecutiveSummaryVO.MonthDataVO;
import com.rxas400adm.as400.vo.ExecutiveSummaryVO.TopEntryVO;
import com.rxas400adm.monitor.mapper.AlertEventMapper;
import com.rxas400adm.monitor.mapper.MetricMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Executive Dashboard 数据聚合服务。
 * 从 MySQL（监控/告警）+ AS400（BPCS 业务数据）汇总高管看板数据。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExecutiveDashboardService {

    private final IIbmiSystemService ibmiSystemService;
    private final AS400ClientProvider clientProvider;
    private final MetricMapper metricMapper;
    private final AlertEventMapper alertEventMapper;

    public ExecutiveSummaryVO getSummary(String cono) {
        // 1. 系统概览（MySQL）
        List<IbmiSystem> servers = ibmiSystemService.list();
        int totalServers = servers.size();
        int onlineServers = (int) servers.stream().filter(s -> "ONLINE".equals(s.getStatus())).count();
        int offlineServers = totalServers - onlineServers;

        // 2. 监控指标（取在线服务器的平均值）
        long totalCpu = 0;
        long totalMemory = 0;
        long totalDisk = 0;
        int validCount = 0;
        for (IbmiSystem server : servers) {
            if ("ONLINE".equals(server.getStatus())) {
                try {
                    List<Map<String, Object>> rows = metricMapper.selectLatestOverview(server.getId());
                    Map<String, Object> overview = new LinkedHashMap<>();
                    for (Map<String, Object> row : rows) {
                        overview.put((String) row.get("metricName"), row.get("metricValue"));
                    }
                    totalCpu += toLong(overview.getOrDefault("CPU", 0L));
                    totalMemory += toLong(overview.getOrDefault("MEMORY", 0L));
                    totalDisk += toLong(overview.getOrDefault("DISK", 0L));
                    validCount++;
                } catch (Exception e) {
                    log.warn("获取服务器 {} 监控数据失败", server.getName(), e);
                }
            }
        }
        Long avgCpu = validCount > 0 ? totalCpu / validCount : 0L;
        Long avgMemory = validCount > 0 ? totalMemory / validCount : 0L;
        Long avgDisk = validCount > 0 ? totalDisk / validCount : 0L;

        // 3. 告警统计（MySQL）
        Long activeAlerts = alertEventMapper.selectCount(null);

        // 4. BPCS 业务概览（AS400）
        Integer totalOrders = 0;
        Integer closedOrders = 0;
        BigDecimal completionRate = BigDecimal.ZERO;
        Integer totalItems = 0;
        BigDecimal inventoryValue = BigDecimal.ZERO;
        BigDecimal onTimeDeliveryRate = BigDecimal.ZERO;
        List<MonthDataVO> salesTrend = Collections.emptyList();
        List<TopEntryVO> topItems = Collections.emptyList();
        List<TopEntryVO> topCustomers = Collections.emptyList();

        try {
            AS400Client client = clientProvider.current();
            // 订单统计
            List<Map<String, Object>> orderStats = client.queryList(
                    "SELECT COUNT(*) AS total, SUM(CASE WHEN CHSTS = '9' THEN 1 ELSE 0 END) AS closed " +
                            "FROM ECHDTA.ECH01 WHERE CONO = ?", cono);
            if (!orderStats.isEmpty()) {
                Map<String, Object> row = orderStats.get(0);
                totalOrders = toInt(row.get("total"));
                closedOrders = toInt(row.get("closed"));
                if (totalOrders > 0) {
                    completionRate = BigDecimal.valueOf(closedOrders)
                            .multiply(BigDecimal.valueOf(100))
                            .divide(BigDecimal.valueOf(totalOrders), 1, RoundingMode.HALF_UP);
                }
            }

            // 物料总数
            List<Map<String, Object>> itemStats = client.queryList(
                    "SELECT COUNT(*) AS cnt FROM ECHDTA.IMIT01 WHERE CONO = ?", cono);
            if (!itemStats.isEmpty()) {
                totalItems = toInt(itemStats.get(0).get("cnt"));
            }

            // 库存总价值
            List<Map<String, Object>> valueStats = client.queryList(
                    "SELECT SUM(IVVAL) AS totalValue FROM ECHDTA.IVMF01 WHERE CONO = ?", cono);
            if (!valueStats.isEmpty()) {
                Object val = valueStats.get(0).get("totalValue");
                inventoryValue = val instanceof Number ? BigDecimal.valueOf(((Number) val).doubleValue()) : BigDecimal.ZERO;
            }

            // OTD（交期达成率）
            List<Map<String, Object>> otdStats = client.queryList(
                    "SELECT COUNT(*) AS total, " +
                            "SUM(CASE WHEN OQDD <= ORQD THEN 1 ELSE 0 END) AS onTime " +
                            "FROM ECHDTA.ECL01 A JOIN ECHDTA.ECH01 B ON A.CONO=B.CONO AND A.ORNO=B.ORNO " +
                            "WHERE A.CONO = ? AND B.CHSTS = '9'", cono);
            if (!otdStats.isEmpty()) {
                Map<String, Object> row = otdStats.get(0);
                int otdTotal = toInt(row.get("total"));
                int onTime = toInt(row.get("onTime"));
                if (otdTotal > 0) {
                    onTimeDeliveryRate = BigDecimal.valueOf(onTime)
                            .multiply(BigDecimal.valueOf(100))
                            .divide(BigDecimal.valueOf(otdTotal), 1, RoundingMode.HALF_UP);
                }
            }

            // 销售趋势（最近 6 个月）
            List<Map<String, Object>> trendRows = client.queryList(
                    "SELECT YMMV AS ym, SUM(OHVAL) AS revenue, COUNT(*) AS orderCount " +
                            "FROM ECHDTA.ECH01 WHERE CONO = ? " +
                            "AND YMMV >= CAST(DIGITS(CURRENT_DATE - 180 DAYS) AS CHAR(6)) " +
                            "GROUP BY YMMV ORDER BY YMMV DESC FETCH FIRST 6 ROWS ONLY", cono);
            salesTrend = new ArrayList<>();
            for (Map<String, Object> row : trendRows) {
                salesTrend.add(new MonthDataVO(
                        (String) row.get("ym"),
                        toBigDecimal(row.get("revenue")),
                        toInt(row.get("orderCount"))));
            }
            Collections.reverse(salesTrend);

            // 热销产品 TOP5
            List<Map<String, Object>> topItemRows = client.queryList(
                    "SELECT ITEM, ITDSC, SUM(OLQTY) AS totalQty, SUM(OLVAL) AS totalAmount " +
                            "FROM ECHDTA.ECL01 A JOIN ECHDTA.IMIT01 B ON A.CONO=B.CONO AND A.ITEM=B.ITEM " +
                            "WHERE A.CONO = ? GROUP BY ITEM, ITDSC ORDER BY totalAmount DESC FETCH FIRST 5 ROWS ONLY", cono);
            topItems = new ArrayList<>();
            for (Map<String, Object> row : topItemRows) {
                topItems.add(new TopEntryVO(
                        (String) row.get("ITEM"),
                        (String) row.get("ITDSC"),
                        toInt(row.get("totalQty")),
                        toBigDecimal(row.get("totalAmount"))));
            }

            // 大客户 TOP5
            List<Map<String, Object>> topCustRows = client.queryList(
                    "SELECT CUST, CUNAM, COUNT(*) AS orderCount, SUM(OHVAL) AS totalAmount " +
                            "FROM ECHDTA.ECH01 A LEFT JOIN ECHDTA.ECCP01 B ON A.CONO=B.CONO AND A.CUST=B.CUST " +
                            "WHERE A.CONO = ? GROUP BY CUST, CUNAM ORDER BY totalAmount DESC FETCH FIRST 5 ROWS ONLY", cono);
            topCustomers = new ArrayList<>();
            for (Map<String, Object> row : topCustRows) {
                topCustomers.add(new TopEntryVO(
                        (String) row.get("CUST"),
                        (String) row.get("CUNAM"),
                        toInt(row.get("orderCount")),
                        toBigDecimal(row.get("totalAmount"))));
            }

        } catch (Exception e) {
            log.warn("BPCS 业务数据查询失败，返回系统概览: {}", e.getMessage());
        }

        return new ExecutiveSummaryVO(
                totalServers, onlineServers, offlineServers,
                avgCpu, avgMemory, avgDisk, activeAlerts, 0L,
                totalOrders, closedOrders, completionRate,
                totalItems, inventoryValue, onTimeDeliveryRate,
                salesTrend, topItems, topCustomers);
    }

    private static long toLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        if (value == null) return 0L;
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static int toInt(Object value) {
        return (int) toLong(value);
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        if (value == null) return BigDecimal.ZERO;
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
