package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.sql.SqlStatementRegistry;
import com.rxas400adm.as400.util.BpcsRowUtil;
import com.rxas400adm.as400.vo.BpcsCreditHoldVO;
import com.rxas400adm.common.config.ProfileResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.rxas400adm.as400.util.BpcsRowUtil.pickStr;

/**
 * ⑪ 信用 Hold 管理实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BpcsCreditHoldServiceImpl implements IBpcsCreditHoldService {

    private final AS400ClientProvider clientProvider;
    private final SqlStatementRegistry statements;
    private final ProfileResolver profileResolver;

    @Override
    public List<BpcsCreditHoldVO> listHoldOrders(String cono, int limit) {
        if (cono == null || cono.isBlank()) cono = "001";
        if (profileResolver.isMockMode()) {
            return List.of(
                    new BpcsCreditHoldVO("001", "MO-100234", "20315", "张三科技", "20260820", "20260828", "CH", "0", "Y", "N", "N")
            );
        }
        String sql = statements.get("bpcs.order.creditHold");
        List<Map<String, Object>> rows = clientProvider.current().queryListCheckedBounded(sql, limit, cono, limit);
        List<BpcsCreditHoldVO> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            result.add(new BpcsCreditHoldVO(
                    pickStr(row, "CONO"), pickStr(row, "ORNO"), pickStr(row, "CUST"),
                    pickStr(row, "CUNAME"), BpcsRowUtil.dateStr(row, "ORDTE"),
                    BpcsRowUtil.dateStr(row, "REQDTE"), pickStr(row, "HID"), pickStr(row, "HSTAT"),
                    pickStr(row, "CRHOLD"), pickStr(row, "SHPHOLD"), pickStr(row, "PRHOLD")
            ));
        }
        return result;
    }
}
