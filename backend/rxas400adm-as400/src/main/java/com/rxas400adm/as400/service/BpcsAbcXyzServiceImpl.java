package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.sql.SqlStatementRegistry;
import com.rxas400adm.as400.util.BpcsRowUtil;
import com.rxas400adm.as400.vo.BpcsInventoryAbcXyzVO;
import com.rxas400adm.common.config.ProfileResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.rxas400adm.as400.util.BpcsRowUtil.pickStr;

/**
 * ABC/XYZ 矩阵分析服务实现（㊲）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BpcsAbcXyzServiceImpl implements IBpcsAbcXyzService {

    private final AS400ClientProvider clientProvider;
    private final SqlStatementRegistry statements;
    private final ProfileResolver profileResolver;

    @Override
    public List<BpcsInventoryAbcXyzVO> getMatrix(String cono, String fromDate, int limit) {
        if (profileResolver.isMockMode()) {
            return mockMatrix();
        }

        // 1. ABC 分析
        String abcSql = statements.get("bpcs.inv.abcAnalysis");
        List<Map<String, Object>> abcRows = clientProvider.current().queryListCheckedBounded(abcSql, limit, cono);

        // 计算总价值
        BigDecimal totalValue = BigDecimal.ZERO;
        List<Map<String, Object>> enrichedRows = new ArrayList<>();
        for (Map<String, Object> row : abcRows) {
            BigDecimal sv = BpcsRowUtil.decOrNull(row, "STOCK_VALUE");
            if (sv != null) totalValue = totalValue.add(sv);
            enrichedRows.add(row);
        }

        // ABC 分类
        BigDecimal cumulative = BigDecimal.ZERO;
        for (Map<String, Object> row : enrichedRows) {
            BigDecimal sv = BpcsRowUtil.decOrNull(row, "STOCK_VALUE");
            if (sv != null && totalValue.compareTo(BigDecimal.ZERO) > 0) {
                cumulative = cumulative.add(sv);
                BigDecimal pct = cumulative.multiply(BigDecimal.valueOf(100)).divide(totalValue, 1, RoundingMode.HALF_UP);
                String abc;
                if (pct.compareTo(new BigDecimal("80")) <= 0) abc = "A";
                else if (pct.compareTo(new BigDecimal("95")) <= 0) abc = "B";
                else abc = "C";
                row.put("_abc", abc);
            } else {
                row.put("_abc", "C");
            }
        }

        // 2. XYZ 分析
        String xyzSql = statements.get("bpcs.inv.xyzAnalysis");
        List<Map<String, Object>> xyzRows = clientProvider.current().queryListCheckedBounded(xyzSql, limit, cono, fromDate);
        Map<String, Map<String, Object>> xyzMap = new HashMap<>();
        for (Map<String, Object> row : xyzRows) {
            xyzMap.put(pickStr(row, "ITEM"), row);
        }

        // 3. 合并
        List<BpcsInventoryAbcXyzVO> result = new ArrayList<>();
        for (Map<String, Object> row : enrichedRows) {
            String item = pickStr(row, "ITEM");
            Map<String, Object> xyzRow = xyzMap.get(item);
            BigDecimal cv = xyzRow != null ? BpcsRowUtil.decOrNull(xyzRow, "CV") : null;
            String xyz = classifyXyz(cv);
            String abc = (String) row.get("_abc");
            String matrix = abc + xyz;

            result.add(new BpcsInventoryAbcXyzVO(
                    item,
                    pickStr(row, "ITDSC"),
                    BpcsRowUtil.intOrNull(row, "TOTAL_QTY"),
                    BpcsRowUtil.decOrNull(row, "STOCK_VALUE"),
                    abc,
                    xyzRow != null ? BpcsRowUtil.decOrNull(xyzRow, "AVG_DEMAND") : null,
                    cv,
                    xyz,
                    matrix
            ));
        }
        return result;
    }

    private String classifyXyz(BigDecimal cv) {
        if (cv == null) return "Z";
        if (cv.compareTo(new BigDecimal("0.5")) < 0) return "X";
        if (cv.compareTo(new BigDecimal("1.0")) <= 0) return "Y";
        return "Z";
    }

    private List<BpcsInventoryAbcXyzVO> mockMatrix() {
        return List.of(
                new BpcsInventoryAbcXyzVO("DEF-2001", "螺柱 M12x30", 5000, new java.math.BigDecimal("90000.00"), "A", new java.math.BigDecimal("200"), new java.math.BigDecimal("0.3"), "X", "AX"),
                new BpcsInventoryAbcXyzVO("DEF-2002", "螺母 M12", 8000, new java.math.BigDecimal("64000.00"), "A", new java.math.BigDecimal("350"), new java.math.BigDecimal("0.6"), "Y", "AY"),
                new BpcsInventoryAbcXyzVO("DEF-2005", "轴承 6205", 300, new java.math.BigDecimal("45000.00"), "A", new java.math.BigDecimal("15"), new java.math.BigDecimal("1.2"), "Z", "AZ"),
                new BpcsInventoryAbcXyzVO("DEF-2003", "垫片 M12", 20000, new java.math.BigDecimal("10000.00"), "B", new java.math.BigDecimal("800"), new java.math.BigDecimal("0.4"), "X", "BX"),
                new BpcsInventoryAbcXyzVO("DEF-2007", "密封圈套装", 500, new java.math.BigDecimal("6000.00"), "B", new java.math.BigDecimal("20"), new java.math.BigDecimal("0.8"), "Y", "BY"),
                new BpcsInventoryAbcXyzVO("DEF-2010", "旧型号轴承", 50, new java.math.BigDecimal("4250.00"), "C", new java.math.BigDecimal("2"), new java.math.BigDecimal("1.5"), "Z", "CZ")
        );
    }
}
