package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.MockBpcsData;
import com.rxas400adm.as400.util.BpcsRowUtil;
import com.rxas400adm.as400.dto.BpcsCustomerQueryDTO;
import com.rxas400adm.as400.service.BpcsOrderServiceImpl.SysConfigServiceHolder;
import com.rxas400adm.as400.sql.SqlStatementRegistry;
import com.rxas400adm.as400.vo.BpcsCustomerVO;
import com.rxas400adm.common.config.ProfileResolver;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 【AS400 业务增强·P2】客户档案查询实现。
 * 数据源：RCM（客户主档）+ EST（收货点），只读访问。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BpcsCustomerServiceImpl implements IBpcsCustomerService {

    private final AS400ClientProvider clientProvider;
    private final ProfileResolver profileResolver;
    private final SqlStatementRegistry statements;
    private final SysConfigServiceHolder configHolder;

    @Override
    public PageResult<BpcsCustomerVO> search(BpcsCustomerQueryDTO query) {
        if (profileResolver.isMockMode()) {
            return mockSearch(query);
        }
        // 真机：查询 RCM 主表
        String lib = configHolder.get(BpcsOrderServiceImpl.KEY_LIBRARY, "BPCSF");
        String sql = statements.get("bpcs.customer.search").replace("{lib}", lib);
        List<Map<String, Object>> rows = clientProvider.current()
                .queryListCheckedBounded(sql, 100,
                        query.getCono() != null ? query.getCono() : "",
                        query.getCust() != null ? query.getCust() : "%",
                        query.getName() != null ? "%" + query.getName() + "%" : "%");
        List<BpcsCustomerVO> all = rows.stream().map(this::toCustomerVO).collect(Collectors.toList());
        return new PageResult<>(all.size(), all);
    }

    @Override
    public BpcsCustomerVO getDetail(String cono, String cust) {
        BpcsCustomerQueryDTO q = new BpcsCustomerQueryDTO();
        q.setCono(cono);
        q.setCust(cust);
        q.setSize(1000); // 详情查询取全部匹配
        return search(q).getRecords().stream().findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "客户不存在: " + cono + "/" + cust));
    }

    private PageResult<BpcsCustomerVO> mockSearch(BpcsCustomerQueryDTO query) {
        String cono = query.getCono();
        String cust = query.getCust();
        String name = query.getName();
        List<Map<String, Object>> masters = MockBpcsData.customerMasters();
        List<Map<String, Object>> shipTos = MockBpcsData.customerShipTos();

        // 按 cust 精确或模糊过滤
        List<Map<String, Object>> matched = new ArrayList<>();
        for (Map<String, Object> row : masters) {
            if (cono != null && !cono.isBlank() && !BpcsRowUtil.strEq(row, "CONO", cono)) continue;
            if (cust != null && !cust.isBlank() && !BpcsRowUtil.strContains(row, "CUST", cust)) continue;
            if (name != null && !name.isBlank() && !BpcsRowUtil.strContains(row, "CUNAME", name)) continue;
            matched.add(row);
        }

        long total = matched.size();
        // 分页截取
        int offset = (query.getCurrent() - 1) * query.getSize();
        List<Map<String, Object>> paged = matched.stream()
                .skip(offset).limit(query.getSize()).collect(Collectors.toList());

        // 组装收货点
        List<BpcsCustomerVO> records = paged.stream().map(row -> {
            String c = BpcsRowUtil.pickStr(row, "CUST");
            List<Map<String, Object>> estRows = shipTos.stream()
                    .filter(s -> BpcsRowUtil.strEq(s, "CUST", c))
                    .collect(Collectors.toList());
            return toCustomerVOWithShipTos(row, estRows);
        }).collect(Collectors.toList());

        return new PageResult<>(total, records);
    }

    private BpcsCustomerVO toCustomerVO(Map<String, Object> row) {
        return toCustomerVOWithShipTos(row, List.of());
    }

    private BpcsCustomerVO toCustomerVOWithShipTos(Map<String, Object> row,
                                                     List<Map<String, Object>> estRows) {
        List<BpcsCustomerVO.ShipToVO> tos = estRows.stream()
                .map(e -> new BpcsCustomerVO.ShipToVO(
                        BpcsRowUtil.pickStr(e, "SHIP"), BpcsRowUtil.pickStr(e, "SHNAME"),
                        BpcsRowUtil.pickStr(e, "SHADDR1"), BpcsRowUtil.pickStr(e, "SHCITY"),
                        BpcsRowUtil.pickStr(e, "SHSTATE"), BpcsRowUtil.pickStr(e, "SHZIP"),
                        BpcsRowUtil.pickStr(e, "SHPHONE")))
                .collect(Collectors.toList());

        return new BpcsCustomerVO(
                BpcsRowUtil.pickStr(row, "CONO"),
                BpcsRowUtil.pickStr(row, "CUST"),
                BpcsRowUtil.pickStr(row, "CUNAME"),
                BpcsRowUtil.pickStr(row, "ADDR1"),
                BpcsRowUtil.pickStr(row, "ADDR2"),
                BpcsRowUtil.pickStr(row, "CITY"),
                BpcsRowUtil.pickStr(row, "STATE"),
                BpcsRowUtil.pickStr(row, "ZIP"),
                BpcsRowUtil.pickStr(row, "PHONE"),
                BpcsRowUtil.pickStr(row, "CONTACT"),
                BpcsRowUtil.decOrNull(row, "CRLMT"),
                BpcsRowUtil.pickStr(row, "TERMS"),
                BpcsRowUtil.pickStr(row, "TAX"),
                BpcsRowUtil.pickStr(row, "SAREA"),
                tos);
    }
}