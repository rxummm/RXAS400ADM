package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.sql.SqlStatementRegistry;
import com.rxas400adm.as400.util.As400PaginationHelper;
import com.rxas400adm.as400.util.BpcsRowUtil;
import com.rxas400adm.as400.vo.BpcsPoLifecycleVO;
import com.rxas400adm.common.config.ProfileResolver;
import com.rxas400adm.common.response.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.rxas400adm.as400.util.BpcsRowUtil.pickStr;

/**
 * ⑤ PO 全生命周期实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BpcsPoLifecycleServiceImpl implements IBpcsPoLifecycleService {

    private final AS400ClientProvider clientProvider;
    private final SqlStatementRegistry statements;
    private final ProfileResolver profileResolver;

    @Override
    public PageResult<BpcsPoLifecycleVO> listPoLifecycle(String cono, int current, int size) {
        if (cono == null || cono.isBlank()) cono = "001";
        if (profileResolver.isMockMode()) {
            var mockData = java.util.List.of(
                    new BpcsPoLifecycleVO("001", "PO-100001", "V001", "供应商A", "20260820", null, "ACTIVE", false, 5),
                    new BpcsPoLifecycleVO("001", "PO-100002", "V002", "供应商B", "20260821", "20260828", "CLOSED", false, 3)
            );
            return new PageResult<>(mockData.size(), mockData);
        }
        String sql = statements.get("bpcs.po.lifecycle");
        return As400PaginationHelper.queryPaged(clientProvider, sql, current, size,
                new Object[]{cono}, row -> new BpcsPoLifecycleVO(
                        pickStr(row, "CONO"), pickStr(row, "PONO"), pickStr(row, "VENDOR"),
                        pickStr(row, "VNAME"), BpcsRowUtil.dateStr(row, "POORD"),
                        BpcsRowUtil.dateStr(row, "POREC"), pickStr(row, "STATUS"),
                        "HOLD".equals(pickStr(row, "STATUS")),
                        BpcsRowUtil.intOrNull(row, "LINE_COUNT")
                ));
    }
}
