package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.sql.SqlStatementRegistry;
import com.rxas400adm.as400.util.BpcsRowUtil;
import com.rxas400adm.as400.vo.BpcsBomLineVO;
import com.rxas400adm.common.constants.As400Identifiers;
import com.rxas400adm.common.config.ProfileResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.rxas400adm.as400.util.BpcsRowUtil.pickStr;

/**
 * ① BOM 查询展开实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BpcsBomServiceImpl implements IBpcsBomService {

    private final AS400ClientProvider clientProvider;
    private final SqlStatementRegistry statements;
    private final ProfileResolver profileResolver;

    @Override
    public List<BpcsBomLineVO> findParents(String cono, String component) {
        validate(cono, component);
        if (profileResolver.isMockMode()) {
            return mockBom();
        }
        String sql = statements.get("bpcs.bom.parents");
        List<Map<String, Object>> rows = clientProvider.current().queryListCheckedBounded(sql, 200, cono, component);
        return mapRows(rows);
    }

    @Override
    public List<BpcsBomLineVO> expandChildren(String cono, String parent) {
        validate(cono, parent);
        if (profileResolver.isMockMode()) {
            return mockBom();
        }
        String sql = statements.get("bpcs.bom.detail");
        List<Map<String, Object>> rows = clientProvider.current().queryListCheckedBounded(sql, 500, cono, parent);
        return mapRows(rows);
    }

    private List<BpcsBomLineVO> mapRows(List<Map<String, Object>> rows) {
        List<BpcsBomLineVO> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            result.add(new BpcsBomLineVO(
                    pickStr(row, "PARENT"),
                    pickStr(row, "COMPONENT"),
                    pickStr(row, "ITDSC"),
                    BpcsRowUtil.decOrNull(row, "QTY"),
                    pickStr(row, "UOM"),
                    BpcsRowUtil.dateStr(row, "EFFECTIVE"),
                    BpcsRowUtil.dateStr(row, "EXPIRED"),
                    BpcsRowUtil.intOrNull(row, "QTYOH"),
                    BpcsRowUtil.intOrNull(row, "QTYALC"),
                    BpcsRowUtil.intOrNull(row, "QTYAVL")
            ));
        }
        return result;
    }

    private List<BpcsBomLineVO> mockBom() {
        return List.of(
                new BpcsBomLineVO("ASM-100", "DEF-2001", "螺柱 M12", new java.math.BigDecimal("4"), "PCS", "20250101", null, 5000, 200, 4800),
                new BpcsBomLineVO("ASM-100", "DEF-2002", "螺母 M12", new java.math.BigDecimal("4"), "PCS", "20250101", null, 8000, 300, 7700),
                new BpcsBomLineVO("ASM-100", "DEF-2003", "垫片 M12", new java.math.BigDecimal("4"), "PCS", "20250101", null, 10000, 500, 9500)
        );
    }

    private void validate(String cono, String item) {
        if (cono == null || cono.isBlank()) cono = "001";
        if (item == null || item.isBlank()) {
            throw new com.rxas400adm.common.exception.BusinessException(
                    com.rxas400adm.common.exception.ErrorCode.BAD_REQUEST, "物料号不能为空");
        }
        if (!As400Identifiers.IDENTIFIER.matcher(cono.toUpperCase()).matches()) {
            throw new com.rxas400adm.common.exception.BusinessException(
                    com.rxas400adm.common.exception.ErrorCode.BAD_REQUEST, "无效的公司码: " + cono);
        }
    }
}
