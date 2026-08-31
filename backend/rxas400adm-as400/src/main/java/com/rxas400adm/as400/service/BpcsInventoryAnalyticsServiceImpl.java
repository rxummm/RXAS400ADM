package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.sql.SqlStatementRegistry;
import com.rxas400adm.as400.util.BpcsRowUtil;
import com.rxas400adm.as400.vo.BpcsInventoryConsistencyVO;
import com.rxas400adm.as400.vo.BpcsInventoryConsistencyVO.LevelDetail;
import com.rxas400adm.as400.vo.BpcsInventoryConsistencyVO.LevelSummary;
import com.rxas400adm.as400.vo.BpcsInventorySlowMovingVO;
import com.rxas400adm.common.constants.As400Identifiers;
import com.rxas400adm.common.config.ProfileResolver;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.rxas400adm.as400.util.BpcsRowUtil.pickStr;

/**
 * 库存分析服务实现（多级一致性核对、呆滞物料分析）。
 *
 * <p>只读查询，数据源 BPCS IPI/ILI/IWM/IWI/IIM/ITL。
 * mock 模式返回演示数据，prod 模式走 AS400 SQL。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BpcsInventoryAnalyticsServiceImpl implements IBpcsInventoryAnalyticsService {

    private final AS400ClientProvider clientProvider;
    private final SqlStatementRegistry statements;
    private final ProfileResolver profileResolver;

    @Override
    public BpcsInventoryConsistencyVO checkConsistency(String cono, String item) {
        validateIdentifiers(cono, item);
        if (profileResolver.isMockMode()) {
            return mockConsistency();
        }
        int ipiQty = queryLevelSum("bpcs.inv.ipiSum", cono, item);
        int ipiCount = queryLevelCount("bpcs.inv.ipiSum", cono, item, "PALLET_COUNT");
        int iliQty = queryLevelSum("bpcs.inv.iliSum", cono, item);
        int iliCount = queryLevelCount("bpcs.inv.iliSum", cono, item, "LOCATION_COUNT");
        int iwmQty = queryLevelSum("bpcs.inv.iwmGet", cono, item);
        int iwmCount = queryLevelCount("bpcs.inv.iwmGet", cono, item, "RECORD_COUNT");

        boolean consistent = (ipiQty == iliQty) && (iliQty == iwmQty);
        String note = consistent ? "三级库存一致" : String.format("IPI(%d) vs ILI(%d) vs IWM(%d) 存在差异", ipiQty, iliQty, iwmQty);

        return new BpcsInventoryConsistencyVO(
                item,
                new LevelSummary("Pallet (IPI)", ipiQty, ipiCount, ipiQty == iliQty, List.of()),
                new LevelSummary("Location (ILI)", iliQty, iliCount, iliQty == iwmQty, List.of()),
                new LevelSummary("Warehouse (IWM)", iwmQty, iwmCount, true, List.of()),
                consistent,
                note
        );
    }

    @Override
    public List<BpcsInventorySlowMovingVO> getSlowMovingItems(String cono, String cutoffDate, int limit) {
        validateCono(cono);
        if (profileResolver.isMockMode()) {
            return mockSlowMoving();
        }
        String sql = statements.get("bpcs.inv.slowMoving");
        List<Map<String, Object>> rows = clientProvider.current().queryListCheckedBounded(sql, limit, cono, cutoffDate);
        List<BpcsInventorySlowMovingVO> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            int idleDays = BpcsRowUtil.intOrNull(row, "IDLE_DAYS");
            String level;
            if (idleDays > 365) level = "OVER_12M";
            else if (idleDays > 180) level = "6M_12M";
            else if (idleDays > 90) level = "3M_6M";
            else level = "UNDER_3M";

            result.add(new BpcsInventorySlowMovingVO(
                    pickStr(row, "ITEM"),
                    pickStr(row, "ITDSC"),
                    pickStr(row, "WH"),
                    BpcsRowUtil.intOrNull(row, "QTYOH"),
                    pickStr(row, "UNIT"),
                    BpcsRowUtil.decOrNull(row, "UNITCOST"),
                    BpcsRowUtil.decOrNull(row, "STOCK_VALUE"),
                    pickStr(row, "LAST_TXN_DATE"),
                    idleDays,
                    level
            ));
        }
        return result;
    }

    // ==================== Mock 数据 ====================

    private BpcsInventoryConsistencyVO mockConsistency() {
        return new BpcsInventoryConsistencyVO(
                "DEF-2001",
                new LevelSummary("Pallet (IPI)", 1250, 12, true, List.of(
                        new LevelDetail("PLT-001", 200, "WH1-A01"),
                        new LevelDetail("PLT-002", 150, "WH1-A02")
                )),
                new LevelSummary("Location (ILI)", 1250, 8, true, List.of(
                        new LevelDetail("A01-01", 300, "PLT-001"),
                        new LevelDetail("A02-01", 250, "PLT-002")
                )),
                new LevelSummary("Warehouse (IWM)", 1250, 1, true, List.of()),
                true,
                "三级库存一致，数量 = 1,250"
        );
    }

    private List<BpcsInventorySlowMovingVO> mockSlowMoving() {
        return List.of(
                new BpcsInventorySlowMovingVO("DEF-2003", "垫片 M12", "WH1", 5000, "PCS", new BigDecimal("0.50"), new BigDecimal("2500.00"), "20250115", 225, "6M_12M"),
                new BpcsInventorySlowMovingVO("DEF-2007", "密封圈套装", "WH2", 200, "SET", new BigDecimal("12.00"), new BigDecimal("2400.00"), "20241201", 270, "6M_12M"),
                new BpcsInventorySlowMovingVO("DEF-2010", "旧型号轴承", "WH1", 50, "PCS", new BigDecimal("85.00"), new BigDecimal("4250.00"), "20240601", 453, "OVER_12M")
        );
    }

    // ==================== 工具方法 ====================

    private int queryLevelSum(String sqlId, String cono, String item) {
        String sql = statements.get(sqlId);
        List<Map<String, Object>> rows = clientProvider.current().queryListChecked(sql, cono, item);
        if (rows.isEmpty()) return 0;
        return BpcsRowUtil.intOrNull(rows.get(0), "TOTAL_QTY");
    }

    private int queryLevelCount(String sqlId, String cono, String item, String countCol) {
        String sql = statements.get(sqlId);
        List<Map<String, Object>> rows = clientProvider.current().queryListChecked(sql, cono, item);
        if (rows.isEmpty()) return 0;
        return BpcsRowUtil.intOrNull(rows.get(0), countCol);
    }

    private void validateIdentifiers(String cono, String item) {
        validateCono(cono);
        if (item == null || item.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "物料号不能为空");
        }
        if (!As400Identifiers.IDENTIFIER.matcher(item.toUpperCase()).matches()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "无效的物料号: " + item);
        }
    }

    private void validateCono(String cono) {
        if (cono == null || cono.isBlank()) {
            cono = "001";
        }
        if (!As400Identifiers.IDENTIFIER.matcher(cono.toUpperCase()).matches()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "无效的公司码: " + cono);
        }
    }
}
