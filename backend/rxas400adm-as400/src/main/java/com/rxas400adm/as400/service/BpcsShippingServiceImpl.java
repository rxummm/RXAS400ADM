package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.util.BpcsRowUtil;
import com.rxas400adm.as400.MockBpcsData;
import com.rxas400adm.as400.dto.BpcsShippingQueryDTO;
import com.rxas400adm.as400.service.BpcsOrderServiceImpl.SysConfigServiceHolder;
import com.rxas400adm.as400.sql.SqlStatementRegistry;
import com.rxas400adm.as400.vo.BpcsLoadVO;
import com.rxas400adm.common.config.ProfileResolver;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.common.util.BpcsDateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 【AS400 业务增强·P2】发运/载荷看板实现。
 * 数据源：LLH（载荷头），只读访问。
 * LHSTAT: 0=Planned, 1=Firmed, 2=Released, 3=Dispatched
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BpcsShippingServiceImpl implements IBpcsShippingService {

    static final String[] LOAD_STATUS_KEYS = {
            "planned", "firmed", "released", "dispatched"
    };

    private final AS400ClientProvider clientProvider;
    private final ProfileResolver profileResolver;
    private final SqlStatementRegistry statements;
    private final SysConfigServiceHolder configHolder;

    @Override
    public PageResult<BpcsLoadVO> search(BpcsShippingQueryDTO query) {
        if (profileResolver.isMockMode()) {
            return mockSearch(query);
        }
        String lib = configHolder.get(BpcsOrderServiceImpl.KEY_LIBRARY, "BPCSF");
        String sql = statements.get("bpcs.shipping.search").replace("{lib}", lib);
        String lhnoFilter = query.getLhno() != null ? "%" + query.getLhno() + "%" : "%";
        List<Map<String, Object>> rows = clientProvider.current()
                .queryListCheckedBounded(sql, 200,
                        query.getCono() != null ? query.getCono() : "",
                        lhnoFilter);
        List<BpcsLoadVO> all = rows.stream().map(this::toLoadVO).collect(Collectors.toList());
        return new PageResult<>(all.size(), all);
    }

    private PageResult<BpcsLoadVO> mockSearch(BpcsShippingQueryDTO query) {
        List<Map<String, Object>> all = MockBpcsData.loadHeaders();
        List<Map<String, Object>> matched = new ArrayList<>();
        for (Map<String, Object> row : all) {
            if (query.getCono() != null && !query.getCono().isBlank()
                    && !BpcsRowUtil.strEq(row, "CONO", query.getCono())) continue;
            if (query.getLhno() != null && !query.getLhno().isBlank()
                    && !BpcsRowUtil.strContains(row, "LHNO", query.getLhno())) continue;
            matched.add(row);
        }
        List<BpcsLoadVO> voList = matched.stream().map(this::toLoadVO).collect(Collectors.toList());
        long total = voList.size();
        int offset = (query.getCurrent() - 1) * query.getSize();
        List<BpcsLoadVO> paged = voList.stream()
                .skip(offset).limit(query.getSize()).collect(Collectors.toList());
        return new PageResult<>(total, paged);
    }

    private BpcsLoadVO toLoadVO(Map<String, Object> row) {
        int stat = BpcsRowUtil.intVal(row, "LHSTAT");
        String statKey = LOAD_STATUS_KEYS[Math.max(0, Math.min(stat, LOAD_STATUS_KEYS.length - 1))];
        String ordnos = BpcsRowUtil.pickStr(row, "ORDNOS");
        List<String> orders = ordnos != null ? List.of(ordnos.split(",")) : List.of();

        return new BpcsLoadVO(
                BpcsRowUtil.pickStr(row, "CONO"),
                BpcsRowUtil.pickStr(row, "LHNO"),
                stat,
                "bpcs.loadStatus." + statKey,
                BpcsRowUtil.pickStr(row, "CARRIER"),
                BpcsRowUtil.pickStr(row, "DEST"),
                BpcsRowUtil.dateStr(row, "SHIPDTE"),
                orders,
                BpcsRowUtil.intVal(row, "LINECT"),
                doubleVal(row, "WEIGHT"));
    }






    private static double doubleVal(Map<String, Object> row, String key) {
        for (Map.Entry<String, Object> en : row.entrySet()) {
            if (en.getKey() != null && en.getKey().equalsIgnoreCase(key)
                    && en.getValue() instanceof Number n) {
                return n.doubleValue();
            }
        }
        return 0.0;
    }

}
