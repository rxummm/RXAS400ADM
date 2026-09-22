package com.rxas400adm.as400.service;

import com.rxas400adm.as400.dto.RmaDTO;
import com.rxas400adm.as400.entity.Rma;
import com.rxas400adm.common.response.PageResult;

/**
 * 退货 RMA 服务接口（CRUD）。
 */
public interface IRmaService {
    PageResult<Rma> list(String status, int current, int size);
    Rma get(Long id);
    Rma create(RmaDTO dto, String operator);
    Rma updateStatus(Long id, String status, String operator);
}
