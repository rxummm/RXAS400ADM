package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.util.BpcsRowUtil;
import com.rxas400adm.as400.MockBpcsData;
import com.rxas400adm.as400.dto.BpcsSalesQueryDTO;
import com.rxas400adm.as400.service.BpcsOrderServiceImpl.SysConfigServiceHolder;
import com.rxas400adm.as400.sql.SqlStatementRegistry;
import com.rxas400adm.as400.vo.BpcsSalesTrendVO;
import com.rxas400adm.common.config.ProfileResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 【AS400 业务增强·P2】销售趋势实现。
 * 数据源 SSH/SSD 按月聚合。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BpcsSalesServiceImpl implements IBpcsSalesService {

    private final AS400ClientProvider clientProvider;
    private final ProfileResolver profileResolver;
    private final SqlStatementRegistry statements;
    private final SysConfigServiceHolder configHolder;

    @Override
    public BpcsSalesTrendVO getTrend(BpcsSalesQueryDTO query) {
        List<Map<String, Object>> rows;
        if (profileResolver.isMockMode()) {
            rows = mockData(query);
        } else {
            String lib = configHolder.get(BpcsOrderServiceImpl.KEY_LIBRARY, "BPCSF");
            String sql = statements.get("bpcs.sales.trend").replace("{lib}", lib);
            rows = clientProvider.current()
                    .queryListCheckedBounded(sql, 24,
                            query.getFromYm() != null ? query.getFromYm() : "",
                            query.getToYm() != null ? query.getToYm() : "999999");
        }

        List<BpcsSalesTrendVO.MonthData> months = new ArrayList<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        int totalOrders = 0;
        int totalLines = 0;

        for (Map<String, Object> row : rows) {
            String ym = BpcsRowUtil.pickStr(row, "YM");
            BigDecimal rev = BpcsRowUtil.decOrNull(row, "REVENUE");
            int orders = BpcsRowUtil.intVal(row, "ORDERS");
            int lines = BpcsRowUtil.intVal(row, "LINES");
            months.add(new BpcsSalesTrendVO.MonthData(ym, rev, orders, lines));
            if (rev != null) totalRevenue = totalRevenue.add(rev);
            totalOrders += orders;
            totalLines += lines;
        }

        return new BpcsSalesTrendVO(months, totalRevenue, totalOrders, totalLines);
    }

    private List<Map<String, Object>> mockData(BpcsSalesQueryDTO query) {
        List<Map<String, Object>> all = MockBpcsData.salesMonthly();
        if (query.getFromYm() == null && query.getToYm() == null) {
            return all;
        }
        return all.stream().filter(row -> {
            String ym = BpcsRowUtil.pickStr(row, "YM");
            if (ym == null) return false;
            String ymNorm = ym.replace("-", "");
            if (query.getFromYm() != null && ymNorm.compareTo(query.getFromYm()) < 0) return false;
            if (query.getToYm() != null && ymNorm.compareTo(query.getToYm()) > 0) return false;
            return true;
        }).collect(Collectors.toList());
    }

}
