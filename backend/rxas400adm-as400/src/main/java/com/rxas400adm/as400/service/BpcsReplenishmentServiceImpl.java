package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.sql.SqlStatementRegistry;
import com.rxas400adm.as400.util.As400PaginationHelper;
import com.rxas400adm.as400.util.BpcsRowUtil;
import com.rxas400adm.as400.vo.BpcsReplenishmentVO;
import com.rxas400adm.common.config.ProfileResolver;
import com.rxas400adm.common.response.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import static com.rxas400adm.as400.util.BpcsRowUtil.pickStr;

/**
 * ② 智能补货建议实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BpcsReplenishmentServiceImpl implements IBpcsReplenishmentService {

    private final AS400ClientProvider clientProvider;
    private final SqlStatementRegistry statements;
    private final ProfileResolver profileResolver;

    @Override
    public PageResult<BpcsReplenishmentVO> getReplenishmentSuggestions(String cono, int current, int size) {
        if (cono == null || cono.isBlank()) cono = "001";
        if (profileResolver.isMockMode()) {
            var mockData = java.util.List.of(
                    new BpcsReplenishmentVO("DEF-2001", "螺柱 M12", "WH1", 50, 200, 150, new BigDecimal("30")),
                    new BpcsReplenishmentVO("DEF-2005", "密封圈", "WH2", 10, 100, 90, new BigDecimal("15"))
            );
            return new PageResult<>(mockData.size(), mockData);
        }
        String sql = statements.get("bpcs.inv.replenishment");
        return As400PaginationHelper.queryPaged(clientProvider, sql, current, size,
                new Object[]{cono}, row -> new BpcsReplenishmentVO(
                        pickStr(row, "ITEM"),
                        pickStr(row, "ITDSC"),
                        pickStr(row, "WH"),
                        BpcsRowUtil.intOrNull(row, "QTYOH"),
                        BpcsRowUtil.intOrNull(row, "SAFETY"),
                        BpcsRowUtil.intOrNull(row, "SHORTAGE"),
                        BpcsRowUtil.decOrNull(row, "AVG_DEMAND")
                ));
    }
}
