package com.rxas400adm.as400.service;

import com.rxas400adm.as400.dto.RmaDTO;
import com.rxas400adm.as400.entity.Rma;

import java.util.List;

/**
 * 退货 RMA 服务接口（CRUD）。
 */
public interface IRmaService {
    List<Rma> list(String status);
    Rma get(Long id);
    Rma create(RmaDTO dto, String operator);
    Rma updateStatus(Long id, String status, String operator);
}
