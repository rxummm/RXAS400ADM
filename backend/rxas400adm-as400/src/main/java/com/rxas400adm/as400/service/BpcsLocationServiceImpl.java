package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.sql.SqlStatementRegistry;
import com.rxas400adm.as400.util.BpcsRowUtil;
import com.rxas400adm.as400.vo.BpcsLocationInventoryVO;
import com.rxas400adm.common.config.ProfileResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.rxas400adm.as400.util.BpcsRowUtil.pickStr;

/**
 * ㉗ 库位库存可视化实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BpcsLocationServiceImpl implements IBpcsLocationService {

    private final AS400ClientProvider clientProvider;
    private final SqlStatementRegistry statements;
    private final ProfileResolver profileResolver;

    @Override
    public List<BpcsLocationInventoryVO> listLocationInventory(String cono, int limit) {
        if (cono == null || cono.isBlank()) cono = "001";
        if (profileResolver.isMockMode()) {
            return mockLocations();
        }
        String sql = statements.get("bpcs.wms.locationInventory");
        List<Map<String, Object>> rows = clientProvider.current().queryListCheckedBounded(sql, limit, cono, limit);
        List<BpcsLocationInventoryVO> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            result.add(new BpcsLocationInventoryVO(
                    pickStr(row, "WHSE"),
                    pickStr(row, "BINLOC"),
                    pickStr(row, "ITEM"),
                    pickStr(row, "ITDSC"),
                    BpcsRowUtil.intOrNull(row, "QTYOH"),
                    pickStr(row, "STATUS")
            ));
        }
        return result;
    }

    private List<BpcsLocationInventoryVO> mockLocations() {
        return List.of(
                new BpcsLocationInventoryVO("WH1", "A01-01", "DEF-2001", "螺柱 M12", 500, "OK"),
                new BpcsLocationInventoryVO("WH1", "A01-02", "DEF-2002", "螺母 M12", 300, "OK"),
                new BpcsLocationInventoryVO("WH2", "B01-01", "DEF-2003", "垫片 M12", 1000, "OK")
        );
    }
}
