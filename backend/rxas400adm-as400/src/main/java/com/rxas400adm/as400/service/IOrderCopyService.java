package com.rxas400adm.as400.service;

import com.rxas400adm.common.response.ApiResponse;

/**
 * 订单复制服务接口。
 */
public interface IOrderCopyService {
    /** 复制订单，返回新订单号 */
    ApiResponse<String> copyOrder(String cono, String sourceOrno, String operator);
}
