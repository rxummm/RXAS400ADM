package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.sql.SqlStatementRegistry;
import com.rxas400adm.as400.util.BpcsRowUtil;
import com.rxas400adm.as400.vo.BpcsPoLifecycleVO;
import com.rxas400adm.common.config.ProfileResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    public List<BpcsPoLifecycleVO> listPoLifecycle(String cono, int limit) {
        if (cono == null || cono.isBlank()) cono = "001";
        if (profileResolver.isMockMode()) {
            return List.of(
                    new BpcsPoLifecycleVO("001", "PO-100001", "V001", "供应商A", "20260820", null, "ACTIVE", false, 5),
                    new BpcsPoLifecycleVO("001", "PO-100002", "V002", "供应商B", "20260821", "20260828", "CLOSED", false, 3)
            );
        }
        String sql = statements.get("bpcs.po.lifecycle");
        List<Map<String, Object>> rows = clientProvider.current().queryListCheckedBounded(sql, limit, cono, limit);
        List<BpcsPoLifecycleVO> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            result.add(new BpcsPoLifecycleVO(
                    pickStr(row, "CONO"), pickStr(row, "PONO"), pickStr(row, "VENDOR"),
                    pickStr(row, "VNAME"), BpcsRowUtil.dateStr(row, "POORD"),
                    BpcsRowUtil.dateStr(row, "POREC"), pickStr(row, "STATUS"),
                    "HOLD".equals(pickStr(row, "STATUS")),
                    BpcsRowUtil.intOrNull(row, "LINE_COUNT")
            ));
        }
        return result;
    }
}
