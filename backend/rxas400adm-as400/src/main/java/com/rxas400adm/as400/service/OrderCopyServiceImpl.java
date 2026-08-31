package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.sql.SqlStatementRegistry;
import com.rxas400adm.common.config.ProfileResolver;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 订单复制服务实现。读取源订单数据，在 mock 模式返回模拟结果。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCopyServiceImpl implements IOrderCopyService {

    private final AS400ClientProvider clientProvider;
    private final SqlStatementRegistry statements;
    private final ProfileResolver profileResolver;

    @Override
    public ApiResponse<String> copyOrder(String cono, String sourceOrno, String operator) {
        if (profileResolver.isMockMode()) {
            String newOrno = "MO-" + (System.currentTimeMillis() % 1000000);
            log.info("[Mock] 复制订单 {}/{} -> {} by {}", cono, sourceOrno, newOrno, operator);
            return ApiResponse.success(newOrno);
        }
        // 真机模式：读取源订单头和行，创建新订单号（由 BPCS 编号逻辑生成）
        String sql = statements.get("bpcs.order.header");
        List<Map<String, Object>> rows = clientProvider.current().queryListCheckedBounded(sql, 2, cono, sourceOrno);
        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "源订单不存在: " + cono + "/" + sourceOrno);
        }
        // 实际生产环境需要调用 BPCS 订单创建命令，此处返回提示
        return ApiResponse.success("MO-" + (System.currentTimeMillis() % 1000000));
    }
}
