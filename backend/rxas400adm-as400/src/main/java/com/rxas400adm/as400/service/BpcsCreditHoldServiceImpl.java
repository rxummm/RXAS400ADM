package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.sql.SqlStatementRegistry;
import com.rxas400adm.as400.util.As400PaginationHelper;
import com.rxas400adm.as400.util.BpcsRowUtil;
import com.rxas400adm.as400.vo.BpcsCreditHoldVO;
import com.rxas400adm.common.config.ProfileResolver;
import com.rxas400adm.common.response.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
    public PageResult<BpcsCreditHoldVO> listHoldOrders(String cono, int current, int size) {
        if (cono == null || cono.isBlank()) cono = "001";
        if (profileResolver.isMockMode()) {
            var mockData = java.util.List.of(
                    new BpcsCreditHoldVO("001", "MO-100234", "20315", "张三科技", "20260820", "20260828", "CH", "0", "Y", "N", "N")
            );
            return new PageResult<>(mockData.size(), mockData);
        }
        String sql = statements.get("bpcs.order.creditHold");
        return As400PaginationHelper.queryPaged(clientProvider, sql, current, size,
                new Object[]{cono}, row -> new BpcsCreditHoldVO(
                        pickStr(row, "CONO"), pickStr(row, "ORNO"), pickStr(row, "CUST"),
                        pickStr(row, "CUNAME"), BpcsRowUtil.dateStr(row, "ORDTE"),
                        BpcsRowUtil.dateStr(row, "REQDTE"), pickStr(row, "HID"), pickStr(row, "HSTAT"),
                        pickStr(row, "CRHOLD"), pickStr(row, "SHPHOLD"), pickStr(row, "PRHOLD")
                ));
    }
}
