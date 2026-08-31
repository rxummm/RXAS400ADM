package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.sql.SqlStatementRegistry;
import com.rxas400adm.as400.util.BpcsRowUtil;
import com.rxas400adm.as400.vo.BpcsShipmentVO;
import com.rxas400adm.common.config.ProfileResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    public List<BpcsShipmentVO> listShipments(String cono, int limit) {
        if (cono == null || cono.isBlank()) cono = "001";
        if (profileResolver.isMockMode()) {
            return mockShipments();
        }
        String sql = statements.get("bpcs.shipment.list");
        List<Map<String, Object>> rows = clientProvider.current().queryListCheckedBounded(sql, limit, cono, limit);
        List<BpcsShipmentVO> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            result.add(new BpcsShipmentVO(
                    pickStr(row, "LHNO"),
                    pickStr(row, "CARRIER"),
                    pickStr(row, "DEST"),
                    BpcsRowUtil.dateStr(row, "SHIPDTE"),
                    BpcsRowUtil.intOrNull(row, "LINECT"),
                    BpcsRowUtil.decOrNull(row, "WEIGHT") != null ? BpcsRowUtil.decOrNull(row, "WEIGHT").doubleValue() : 0,
                    pickStr(row, "ORDNOS")
            ));
        }
        return result;
    }

    private List<BpcsShipmentVO> mockShipments() {
        return List.of(
                new BpcsShipmentVO("LL-2026001", "FedEx", "上海市浦东新区", "20260828", 5, 120.5, "MO-100234,MO-100235"),
                new BpcsShipmentVO("LL-2026002", "DHL", "北京市朝阳区", "20260829", 3, 85.0, "MO-100236")
        );
    }
}
