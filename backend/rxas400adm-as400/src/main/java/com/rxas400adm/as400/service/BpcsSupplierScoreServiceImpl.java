package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.sql.SqlStatementRegistry;
import com.rxas400adm.as400.util.As400PaginationHelper;
import com.rxas400adm.as400.util.BpcsRowUtil;
import com.rxas400adm.as400.vo.BpcsSupplierScoreVO;
import com.rxas400adm.common.config.ProfileResolver;
import com.rxas400adm.common.response.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.rxas400adm.as400.util.BpcsRowUtil.pickStr;

/**
 * ④ 供应商评分实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BpcsSupplierScoreServiceImpl implements IBpcsSupplierScoreService {

    private final AS400ClientProvider clientProvider;
    private final SqlStatementRegistry statements;
    private final ProfileResolver profileResolver;

    @Override
    public PageResult<BpcsSupplierScoreVO> getSupplierScores(String cono, int current, int size) {
        if (cono == null || cono.isBlank()) cono = "001";
        if (profileResolver.isMockMode()) {
            var mockData = java.util.List.of(
                    new BpcsSupplierScoreVO("V001", "供应商A", 120, 115, 95.8),
                    new BpcsSupplierScoreVO("V002", "供应商B", 80, 68, 85.0)
            );
            return new PageResult<>(mockData.size(), mockData);
        }
        String sql = statements.get("bpcs.purchase.supplierPerf");
        return As400PaginationHelper.queryPaged(clientProvider, sql, current, size,
                new Object[]{cono}, row -> new BpcsSupplierScoreVO(
                        pickStr(row, "VENDOR"),
                        pickStr(row, "VNAME"),
                        BpcsRowUtil.intOrNull(row, "TOTAL_PO"),
                        BpcsRowUtil.intOrNull(row, "ON_TIME"),
                        BpcsRowUtil.decOrNull(row, "SCORE") != null ? BpcsRowUtil.decOrNull(row, "SCORE").doubleValue() : 0
                ));
    }
}
