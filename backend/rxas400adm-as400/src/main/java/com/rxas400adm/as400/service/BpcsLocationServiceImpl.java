package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.sql.SqlStatementRegistry;
import com.rxas400adm.as400.util.As400PaginationHelper;
import com.rxas400adm.as400.util.BpcsRowUtil;
import com.rxas400adm.as400.vo.BpcsLocationInventoryVO;
import com.rxas400adm.common.config.ProfileResolver;
import com.rxas400adm.common.response.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
    public PageResult<BpcsLocationInventoryVO> listLocationInventory(String cono, int current, int size) {
        if (cono == null || cono.isBlank()) cono = "001";
        if (profileResolver.isMockMode()) {
            var mockData = java.util.List.of(
                    new BpcsLocationInventoryVO("WH1", "A01-01", "DEF-2001", "螺柱 M12", 500, "OK"),
                    new BpcsLocationInventoryVO("WH1", "A01-02", "DEF-2002", "螺母 M12", 300, "OK"),
                    new BpcsLocationInventoryVO("WH2", "B01-01", "DEF-2003", "垫片 M12", 1000, "OK")
            );
            return new PageResult<>(mockData.size(), mockData);
        }
        String sql = statements.get("bpcs.wms.locationInventory");
        return As400PaginationHelper.queryPaged(clientProvider, sql, current, size,
                new Object[]{cono}, row -> new BpcsLocationInventoryVO(
                        pickStr(row, "WHSE"),
                        pickStr(row, "BINLOC"),
                        pickStr(row, "ITEM"),
                        pickStr(row, "ITDSC"),
                        BpcsRowUtil.intOrNull(row, "QTYOH"),
                        pickStr(row, "STATUS")
                ));
    }
}
