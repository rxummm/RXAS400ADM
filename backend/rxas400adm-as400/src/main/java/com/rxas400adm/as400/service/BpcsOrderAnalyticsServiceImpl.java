package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.dto.BpcsOrderFulfillmentQueryDTO;
import com.rxas400adm.as400.sql.SqlStatementRegistry;
import com.rxas400adm.as400.util.BpcsRowUtil;
import com.rxas400adm.as400.vo.*;
import com.rxas400adm.common.constants.As400Identifiers;
import com.rxas400adm.common.config.ProfileResolver;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.rxas400adm.as400.util.BpcsRowUtil.pickStr;

/**
 * 订单分析服务实现（履行率、OTD、Backorder）。
 *
 * <p>只读查询，数据源 BPCS ECH/ECL/ESH。
 * mock 模式返回演示数据，prod 模式走 AS400 SQL。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BpcsOrderAnalyticsServiceImpl implements IBpcsOrderAnalyticsService {

    private final AS400ClientProvider clientProvider;
    private final SqlStatementRegistry statements;
    private final ProfileResolver profileResolver;

    @Override
    public BpcsOrderFulfillmentStatsVO getFulfillmentStats(String cono) {
        validateCono(cono);
        if (profileResolver.isMockMode()) {
            return mockFulfillmentStats();
        }
        String sql = statements.get("bpcs.order.fulfillmentStats");
        List<Map<String, Object>> rows = clientProvider.current().queryListChecked(sql, cono);
        if (rows.isEmpty()) {
            return new BpcsOrderFulfillmentStatsVO(0, 0, BigDecimal.ZERO, 0, 0, BigDecimal.ZERO, 0, 0);
        }
        Map<String, Object> row = rows.get(0);
        int total = BpcsRowUtil.intOrNull(row, "TOTAL_LINES");
        int filled = BpcsRowUtil.intOrNull(row, "FILLED_LINES");
        int totalOrdered = BpcsRowUtil.intOrNull(row, "TOTAL_ORDERED");
        int totalShipped = BpcsRowUtil.intOrNull(row, "TOTAL_SHIPPED");
        int boLines = BpcsRowUtil.intOrNull(row, "BACKORDER_LINES");
        int boQty = BpcsRowUtil.intOrNull(row, "BACKORDER_QTY");

        BigDecimal lineRate = total > 0
                ? BigDecimal.valueOf(filled).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal qtyRate = totalOrdered > 0
                ? BigDecimal.valueOf(totalShipped).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(totalOrdered), 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new BpcsOrderFulfillmentStatsVO(total, filled, lineRate, totalOrdered, totalShipped, qtyRate, boLines, boQty);
    }

    @Override
    public List<BpcsOrderBackorderLineVO> getBackorderLines(BpcsOrderFulfillmentQueryDTO query) {
        validateCono(query.getCono());
        if (profileResolver.isMockMode()) {
            return mockBackorderLines();
        }
        String sql = statements.get("bpcs.order.backorderLines");
        List<Map<String, Object>> rows = clientProvider.current().queryListCheckedBounded(sql, query.getSize(), query.getCono());
        List<BpcsOrderBackorderLineVO> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String item = pickStr(row, "ITEM");
            if (query.getItemFilter() != null && !query.getItemFilter().isBlank()
                    && !item.toUpperCase().contains(query.getItemFilter().toUpperCase())) {
                continue;
            }
            result.add(new BpcsOrderBackorderLineVO(
                    pickStr(row, "CONO"),
                    pickStr(row, "ORNO"),
                    pickStr(row, "ORLN"),
                    item,
                    pickStr(row, "ITDSC"),
                    BpcsRowUtil.intOrNull(row, "QTORD"),
                    BpcsRowUtil.intOrNull(row, "QTSHP"),
                    BpcsRowUtil.intOrNull(row, "QTYALC"),
                    BpcsRowUtil.intOrNull(row, "QTYOPEN"),
                    pickStr(row, "CUST"),
                    pickStr(row, "REQDTE"),
                    pickStr(row, "HSTAT")
            ));
        }
        return result;
    }

    @Override
    public List<BpcsOrderBackorderByItemVO> getBackorderByItem(String cono, int limit) {
        validateCono(cono);
        if (profileResolver.isMockMode()) {
            return mockBackorderByItem();
        }
        String sql = statements.get("bpcs.order.backorderByItem");
        List<Map<String, Object>> rows = clientProvider.current().queryListCheckedBounded(sql, limit, cono);
        List<BpcsOrderBackorderByItemVO> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            result.add(new BpcsOrderBackorderByItemVO(
                    pickStr(row, "ITEM"),
                    pickStr(row, "ITDSC"),
                    BpcsRowUtil.intOrNull(row, "BO_COUNT"),
                    BpcsRowUtil.intOrNull(row, "TOTAL_BO_QTY")
            ));
        }
        return result;
    }

    @Override
    public BpcsOrderOtdStatsVO getOtdStats(String cono) {
        validateCono(cono);
        if (profileResolver.isMockMode()) {
            return mockOtdStats();
        }
        String sql = statements.get("bpcs.order.otdStats");
        List<Map<String, Object>> rows = clientProvider.current().queryListChecked(sql, cono);
        if (rows.isEmpty()) {
            return new BpcsOrderOtdStatsVO(0, 0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        Map<String, Object> row = rows.get(0);
        int total = BpcsRowUtil.intOrNull(row, "TOTAL_DELIVERED");
        int onTime = BpcsRowUtil.intOrNull(row, "ON_TIME");
        int early = BpcsRowUtil.intOrNull(row, "EARLY");
        int late = BpcsRowUtil.intOrNull(row, "LATE");
        BigDecimal avgLate = BpcsRowUtil.decOrNull(row, "AVG_LATE_DAYS");
        if (avgLate == null) avgLate = BigDecimal.ZERO;

        BigDecimal rate = total > 0
                ? BigDecimal.valueOf(onTime).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new BpcsOrderOtdStatsVO(total, onTime, early, late, rate, avgLate);
    }

    @Override
    public List<BpcsOrderOtdByCustomerVO> getOtdByCustomer(String cono, int limit) {
        validateCono(cono);
        if (profileResolver.isMockMode()) {
            return mockOtdByCustomer();
        }
        String sql = statements.get("bpcs.order.otdByCustomer");
        List<Map<String, Object>> rows = clientProvider.current().queryListCheckedBounded(sql, limit, cono);
        List<BpcsOrderOtdByCustomerVO> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            result.add(new BpcsOrderOtdByCustomerVO(
                    pickStr(row, "CUST"),
                    pickStr(row, "CUNAME"),
                    BpcsRowUtil.intOrNull(row, "TOTAL"),
                    BpcsRowUtil.intOrNull(row, "ON_TIME"),
                    BpcsRowUtil.decOrNull(row, "OTD_PCT")
            ));
        }
        return result;
    }

    // ==================== Mock 数据 ====================

    private BpcsOrderFulfillmentStatsVO mockFulfillmentStats() {
        return new BpcsOrderFulfillmentStatsVO(
                120, 96, new BigDecimal("80.0"),
                50000, 42000, new BigDecimal("84.0"),
                24, 8000
        );
    }

    private List<BpcsOrderBackorderLineVO> mockBackorderLines() {
        return List.of(
                new BpcsOrderBackorderLineVO("001", "123456", "003", "DEF-2001", "螺柱 M12x30", 500, 200, 300, 300, "20315", "20250701", "0"),
                new BpcsOrderBackorderLineVO("001", "123456", "005", "DEF-2003", "垫片 M12", 1000, 0, 0, 1000, "20315", "20250701", "0"),
                new BpcsOrderBackorderLineVO("001", "234567", "002", "DEF-2005", "轴承 6205", 200, 100, 150, 100, "20777", "20250815", "0")
        );
    }

    private List<BpcsOrderBackorderByItemVO> mockBackorderByItem() {
        return List.of(
                new BpcsOrderBackorderByItemVO("DEF-2003", "垫片 M12", 5, 5000),
                new BpcsOrderBackorderByItemVO("DEF-2001", "螺柱 M12x30", 3, 1500),
                new BpcsOrderBackorderByItemVO("DEF-2005", "轴承 6205", 2, 400)
        );
    }

    private BpcsOrderOtdStatsVO mockOtdStats() {
        return new BpcsOrderOtdStatsVO(
                85, 72, 8, 5,
                new BigDecimal("84.7"),
                new BigDecimal("2.3")
        );
    }

    private List<BpcsOrderOtdByCustomerVO> mockOtdByCustomer() {
        return List.of(
                new BpcsOrderOtdByCustomerVO("20315", "张三科技", 25, 22, new BigDecimal("88.0")),
                new BpcsOrderOtdByCustomerVO("20777", "王五制造", 30, 24, new BigDecimal("80.0")),
                new BpcsOrderOtdByCustomerVO("20100", "赵六贸易", 15, 10, new BigDecimal("66.7"))
        );
    }

    // ==================== 校验 ====================

    private void validateCono(String cono) {
        if (cono == null || cono.isBlank()) {
            cono = "001"; // 默认公司码
        }
        if (!As400Identifiers.IDENTIFIER.matcher(cono.toUpperCase()).matches()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "无效的公司码: " + cono);
        }
    }
}
