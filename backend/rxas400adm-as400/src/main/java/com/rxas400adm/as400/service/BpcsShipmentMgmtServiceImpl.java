package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.sql.SqlStatementRegistry;
import com.rxas400adm.as400.util.As400PaginationHelper;
import com.rxas400adm.as400.util.BpcsRowUtil;
import com.rxas400adm.as400.vo.BpcsShipmentVO;
import com.rxas400adm.common.config.ProfileResolver;
import com.rxas400adm.common.response.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.rxas400adm.as400.util.BpcsRowUtil.pickStr;

/**
 * ㊳ 运单管理实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BpcsShipmentMgmtServiceImpl implements IBpcsShipmentMgmtService {

    private final AS400ClientProvider clientProvider;
    private final SqlStatementRegistry statements;
    private final ProfileResolver profileResolver;

    @Override
    public PageResult<BpcsShipmentVO> listShipments(String cono, int current, int size) {
        if (cono == null || cono.isBlank()) cono = "001";
        if (profileResolver.isMockMode()) {
            var mockData = java.util.List.of(
                    new BpcsShipmentVO("LL-2026001", "FedEx", "上海市浦东新区", "20260828", 5, 120.5, "MO-100234,MO-100235"),
                    new BpcsShipmentVO("LL-2026002", "DHL", "北京市朝阳区", "20260829", 3, 85.0, "MO-100236")
            );
            return new PageResult<>(mockData.size(), mockData);
        }
        String sql = statements.get("bpcs.shipment.list");
        return As400PaginationHelper.queryPaged(clientProvider, sql, current, size,
                new Object[]{cono}, row -> new BpcsShipmentVO(
                        pickStr(row, "LHNO"),
                        pickStr(row, "CARRIER"),
                        pickStr(row, "DEST"),
                        BpcsRowUtil.dateStr(row, "SHIPDTE"),
                        BpcsRowUtil.intOrNull(row, "LINECT"),
                        BpcsRowUtil.decOrNull(row, "WEIGHT") != null ? BpcsRowUtil.decOrNull(row, "WEIGHT").doubleValue() : 0,
                        pickStr(row, "ORDNOS")
                ));
    }
}
