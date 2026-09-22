package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.sql.SqlStatementRegistry;
import com.rxas400adm.as400.util.As400PaginationHelper;
import com.rxas400adm.as400.util.BpcsRowUtil;
import com.rxas400adm.as400.vo.BpcsStockValueVO;
import com.rxas400adm.common.config.ProfileResolver;
import com.rxas400adm.common.response.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import static com.rxas400adm.as400.util.BpcsRowUtil.pickStr;

/**
 * ㉚ 库存价值核算实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BpcsStockValueServiceImpl implements IBpcsStockValueService {

    private final AS400ClientProvider clientProvider;
    private final SqlStatementRegistry statements;
    private final ProfileResolver profileResolver;

    @Override
    public PageResult<BpcsStockValueVO> getValueReport(String cono, int current, int size) {
        if (cono == null || cono.isBlank()) cono = "001";
        if (profileResolver.isMockMode()) {
            var mockData = java.util.List.of(
                    new BpcsStockValueVO("DEF-2001", "螺柱 M12", "WH1", 5000, new BigDecimal("2.50"), new BigDecimal("12500.00")),
                    new BpcsStockValueVO("DEF-2005", "密封圈", "WH2", 200, new BigDecimal("15.00"), new BigDecimal("3000.00"))
            );
            return new PageResult<>(mockData.size(), mockData);
        }
        String sql = statements.get("bpcs.inv.valueReport");
        return As400PaginationHelper.queryPaged(clientProvider, sql, current, size,
                new Object[]{cono}, row -> new BpcsStockValueVO(
                        pickStr(row, "ITEM"),
                        pickStr(row, "ITDSC"),
                        pickStr(row, "WH"),
                        BpcsRowUtil.intOrNull(row, "QTYOH"),
                        BpcsRowUtil.decOrNull(row, "UNITCOST"),
                        BpcsRowUtil.decOrNull(row, "STOCK_VALUE")
                ));
    }
}
