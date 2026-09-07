package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.sql.SqlStatementRegistry;
import com.rxas400adm.as400.util.BpcsRowUtil;
import com.rxas400adm.as400.vo.BpcsOrderAnomalyVO;
import com.rxas400adm.common.constants.As400Identifiers;
import com.rxas400adm.common.config.ProfileResolver;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.rxas400adm.as400.util.BpcsRowUtil.pickStr;

/**
 * ⑨ 异常检测引擎实现。扫描 Hold/Backorder/延迟订单。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BpcsAnomalyDetectionServiceImpl implements IBpcsAnomalyDetectionService {

    private final AS400ClientProvider clientProvider;
    private final SqlStatementRegistry statements;
    private final ProfileResolver profileResolver;

    @Override
    public List<BpcsOrderAnomalyVO> detectAnomalies(String cono, int limit) {
        if (cono == null || cono.isBlank()) {
            cono = "001";
        }
        validate(cono);
        if (profileResolver.isMockMode()) {
            return mockAnomalies();
        }
        String sql = statements.get("bpcs.order.anomalies");
        List<Map<String, Object>> rows = clientProvider.current().queryListCheckedBounded(sql, limit, cono, limit);
        List<BpcsOrderAnomalyVO> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            result.add(new BpcsOrderAnomalyVO(
                    pickStr(row, "CONO"),
                    pickStr(row, "ORNO"),
                    pickStr(row, "CUST"),
                    pickStr(row, "CUNAME"),
                    BpcsRowUtil.dateStr(row, "ORDTE"),
                    BpcsRowUtil.dateStr(row, "REQDTE"),
                    pickStr(row, "STATUS"),
                    BpcsRowUtil.intOrNull(row, "BACKORDER_LINES")
            ));
        }
        return result;
    }

    private List<BpcsOrderAnomalyVO> mockAnomalies() {
        return List.of(
                new BpcsOrderAnomalyVO("001", "MO-100234", "20315", "张三科技", "20260820", "20260828", "HOLD", 2),
                new BpcsOrderAnomalyVO("001", "MO-100235", "20316", "王五制造", "20260821", "20260830", "ACTIVE", 5)
        );
    }

    private void validate(String cono) {
        if (!As400Identifiers.IDENTIFIER.matcher(cono.toUpperCase()).matches()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "无效的公司码: " + cono);
        }
    }
}
