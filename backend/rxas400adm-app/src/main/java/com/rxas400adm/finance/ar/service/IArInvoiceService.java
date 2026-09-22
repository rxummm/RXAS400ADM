package com.rxas400adm.finance.ar.service;

import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.finance.ar.dto.ArInvoiceCreateDTO;
import com.rxas400adm.finance.ar.dto.ArInvoiceQueryDTO;
import com.rxas400adm.finance.ar.dto.ArInvoiceUpdateDTO;
import com.rxas400adm.finance.ar.dto.ArPaymentCreateDTO;
import com.rxas400adm.finance.ar.vo.ArInvoiceDetailVO;
import com.rxas400adm.finance.ar.vo.ArInvoiceVO;
import com.rxas400adm.finance.ar.vo.ArPaymentVO;

/**
 * 应收账款发票服务接口。
 */
public interface IArInvoiceService {

    /**
     * 分页查询应收账款发票。
     */
    PageResult<ArInvoiceVO> pageQuery(ArInvoiceQueryDTO query);

    /**
     * 获取应收账款发票详情（含收款记录）。
     */
    ArInvoiceDetailVO getDetail(Long id);

    /**
     * 新建应收账款发票（DRAFT）。
     */
    ArInvoiceVO create(ArInvoiceCreateDTO dto);

    /**
     * 更新应收账款发票（仅 DRAFT 状态可编辑）。
     */
    ArInvoiceVO update(Long id, ArInvoiceUpdateDTO dto);

    /**
     * 删除应收账款发票（仅 DRAFT 可删除）。
     */
    void delete(Long id);

    /**
     * 提交发票（DRAFT → OPEN）。
     */
    void submit(Long id);

    /**
     * 记录收款（核销）。
     */
    ArPaymentVO receivePayment(ArPaymentCreateDTO dto);

    /**
     * 生成下一个账单号（AR + yyyyMMdd + 4位序号）。
     */
    String generateInvoiceNo();

    /**
     * 生成下一个收款单号（PMT + yyyyMMdd + 4位序号）。
     */
    String generatePaymentNo();
}
