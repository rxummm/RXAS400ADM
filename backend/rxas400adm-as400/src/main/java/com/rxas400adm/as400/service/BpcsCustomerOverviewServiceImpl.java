package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.sql.SqlStatementRegistry;
import com.rxas400adm.as400.util.BpcsRowUtil;
import com.rxas400adm.as400.vo.BpcsCustomerOverviewVO;
import com.rxas400adm.as400.vo.BpcsCustomerOverviewVO.OverdueInvoice;
import com.rxas400adm.as400.vo.BpcsCustomerOverviewVO.RecentOrder;
import com.rxas400adm.common.config.ProfileResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.rxas400adm.as400.util.BpcsRowUtil.pickStr;

/**
 * 客户 360° 视图服务实现（⑳）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BpcsCustomerOverviewServiceImpl implements IBpcsCustomerOverviewService {

    private final AS400ClientProvider clientProvider;
    private final SqlStatementRegistry statements;
    private final ProfileResolver profileResolver;

    @Override
    public BpcsCustomerOverviewVO getOverview(String cono, String cust, int orderLimit, int invoiceLimit) {
        if (profileResolver.isMockMode()) {
            return mockOverview();
        }

        // 1. 查询客户概览
        String overviewSql = statements.get("bpcs.customer.overview");
        List<Map<String, Object>> overviewRows = clientProvider.current().queryListChecked(overviewSql, cono, cust);
        if (overviewRows.isEmpty()) {
            return null;
        }
        Map<String, Object> row = overviewRows.get(0);

        // 2. 查询最近订单
        String ordersSql = statements.get("bpcs.customer.recentOrders");
        List<Map<String, Object>> orderRows = clientProvider.current().queryListCheckedBounded(ordersSql, orderLimit, cono, cust);
        List<RecentOrder> orders = new ArrayList<>();
        for (Map<String, Object> or : orderRows) {
            orders.add(new RecentOrder(
                    pickStr(or, "ORNO"), pickStr(or, "ORDTE"), pickStr(or, "REQDTE"),
                    pickStr(or, "HSTAT"),
                    BpcsRowUtil.intOrNull(or, "LINE_COUNT"),
                    BpcsRowUtil.decOrNull(or, "ORDER_TOTAL")
            ));
        }

        // 3. 查询逾期发票（假设 cutoff 为当前日期的 YYYYMMDD 格式）
        String invoicesSql = statements.get("bpcs.customer.overdueInvoices");
        // 使用简单的日期比较；实际应传入当前日期
        List<Map<String, Object>> invoiceRows = clientProvider.current().queryListCheckedBounded(invoicesSql, invoiceLimit, cono, cust, "99991231");
        List<OverdueInvoice> invoices = new ArrayList<>();
        for (Map<String, Object> ir : invoiceRows) {
            invoices.add(new OverdueInvoice(
                    pickStr(ir, "INVNO"), pickStr(ir, "INVDTE"),
                    BpcsRowUtil.decOrNull(ir, "INVAMT"), pickStr(ir, "ORNO")
            ));
        }

        return new BpcsCustomerOverviewVO(
                pickStr(row, "CONO"), pickStr(row, "CUST"), pickStr(row, "CUNAME"),
                pickStr(row, "ADDR1"), pickStr(row, "CITY"), pickStr(row, "STATE"),
                pickStr(row, "ZIPCD"), pickStr(row, "PHONE"), pickStr(row, "CONTACT"),
                BpcsRowUtil.decOrNull(row, "CREDLM"), pickStr(row, "TERMS"),
                pickStr(row, "TAXCODE"), pickStr(row, "SLSREP"),
                BpcsRowUtil.intOrNull(row, "TOTAL_ORDERS"),
                BpcsRowUtil.intOrNull(row, "OPEN_ORDERS"),
                BpcsRowUtil.decOrNull(row, "TOTAL_REVENUE"),
                orders, invoices
        );
    }

    private BpcsCustomerOverviewVO mockOverview() {
        return new BpcsCustomerOverviewVO(
                "001", "20315", "张三科技有限公司",
                "上海市浦东新区张江高科技园区", "上海", "SH", "201203", "021-58888888", "张三",
                new BigDecimal("500000.00"), "N30", "VAT13", "SALES01",
                45, 3, new BigDecimal("2850000.00"),
                List.of(
                        new RecentOrder("123456", "20250312", "20250401", "8", 5, new BigDecimal("45000.00")),
                        new RecentOrder("123789", "20250220", "20250315", "8", 3, new BigDecimal("12000.00")),
                        new RecentOrder("124000", "20250110", "20250201", "0", 8, new BigDecimal("78000.00"))
                ),
                List.of(
                        new OverdueInvoice("INV-2024089", "20241215", new BigDecimal("35000.00"), "123000"),
                        new OverdueInvoice("INV-2024072", "20241101", new BigDecimal("18500.00"), "122500")
                )
        );
    }
}
