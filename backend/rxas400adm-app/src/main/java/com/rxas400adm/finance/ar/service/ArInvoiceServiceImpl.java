package com.rxas400adm.finance.ar.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.common.util.EntityUtil;
import com.rxas400adm.common.util.SecurityUtils;
import com.rxas400adm.finance.ar.dto.ArInvoiceCreateDTO;
import com.rxas400adm.finance.ar.dto.ArInvoiceQueryDTO;
import com.rxas400adm.finance.ar.dto.ArInvoiceUpdateDTO;
import com.rxas400adm.finance.ar.dto.ArPaymentCreateDTO;
import com.rxas400adm.finance.ar.entity.ArInvoice;
import com.rxas400adm.finance.ar.entity.ArPayment;
import com.rxas400adm.finance.ar.mapper.ArInvoiceMapper;
import com.rxas400adm.finance.ar.mapper.ArPaymentMapper;
import com.rxas400adm.finance.ar.vo.ArInvoiceDetailVO;
import com.rxas400adm.finance.ar.vo.ArInvoiceVO;
import com.rxas400adm.finance.ar.vo.ArPaymentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 应收账款发票服务实现。
 * 支持完整 CRUD、收款核销、账龄分析。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArInvoiceServiceImpl implements IArInvoiceService {

    private final ArInvoiceMapper invoiceMapper;
    private final ArPaymentMapper paymentMapper;

    /**
     * 分页查询应收账款发票。
     */
    @Override
    public PageResult<ArInvoiceVO> pageQuery(ArInvoiceQueryDTO query) {
        LambdaQueryWrapper<ArInvoice> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getInvoiceNo())) {
            wrapper.like(ArInvoice::getInvoiceNo, query.getInvoiceNo());
        }
        if (StringUtils.hasText(query.getCustomerCode())) {
            wrapper.eq(ArInvoice::getCustomerCode, query.getCustomerCode());
        }
        if (StringUtils.hasText(query.getCustomerName())) {
            wrapper.like(ArInvoice::getCustomerName, query.getCustomerName());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(ArInvoice::getStatus, query.getStatus());
        }
        if (query.getInvoiceDateFrom() != null) {
            wrapper.ge(ArInvoice::getInvoiceDate, query.getInvoiceDateFrom());
        }
        if (query.getInvoiceDateTo() != null) {
            wrapper.le(ArInvoice::getInvoiceDate, query.getInvoiceDateTo());
        }
        if (query.getDueDateFrom() != null) {
            wrapper.ge(ArInvoice::getDueDate, query.getDueDateFrom());
        }
        if (query.getDueDateTo() != null) {
            wrapper.le(ArInvoice::getDueDate, query.getDueDateTo());
        }
        wrapper.orderByDesc(ArInvoice::getCreatedTime);

        IPage<ArInvoice> page = invoiceMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<ArInvoiceVO> records = page.getRecords().stream()
                .map(ArInvoiceVO::from).toList();
        return new PageResult<>(page.getTotal(), records);
    }

    /**
     * 获取应收账款发票详情（含收款记录）。
     */
    @Override
    public ArInvoiceDetailVO getDetail(Long id) {
        return toDetailVO(EntityUtil.require(id, "AR invoice", invoiceMapper::selectById));
    }

    /**
     * 新建应收账款发票（DRAFT）。
     */
    @Override
    public ArInvoiceVO create(ArInvoiceCreateDTO dto) {
        String username = SecurityUtils.currentUsername();

        // 校验单号唯一
        Long exists = invoiceMapper.selectCount(new LambdaQueryWrapper<ArInvoice>()
                .eq(ArInvoice::getInvoiceNo, dto.getInvoiceNo()));
        if (exists > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Invoice number already exists");
        }

        // 创建发票
        ArInvoice invoice = new ArInvoice();
        invoice.setInvoiceNo(dto.getInvoiceNo());
        invoice.setCono(dto.getCono() != null ? dto.getCono() : "001");
        invoice.setCustomerCode(dto.getCustomerCode());
        invoice.setCustomerName(dto.getCustomerName());
        invoice.setInvoiceDate(dto.getInvoiceDate());
        invoice.setDueDate(dto.getDueDate());
        invoice.setOriginPoNo(dto.getOriginPoNo());
        invoice.setOriginSoNo(dto.getOriginSoNo());
        invoice.setSubtotal(dto.getSubtotal());
        invoice.setTaxRate(dto.getTaxRate());
        invoice.setTaxAmount(dto.getTaxAmount());
        invoice.setTotalAmount(dto.getTotalAmount());
        invoice.setPaidAmount(BigDecimal.ZERO);
        invoice.setBalance(dto.getTotalAmount());
        invoice.setCurrency(dto.getCurrency() != null ? dto.getCurrency() : "CNY");
        invoice.setStatus("DRAFT");
        invoice.setNotes(dto.getNotes());
        invoice.setCreatedBy(username);
        invoice.setCreatedTime(LocalDateTime.now());
        invoiceMapper.insert(invoice);

        // 计算账龄区间
        updateAgingBucket(invoice);
        invoiceMapper.updateById(invoice);

        log.info("应收账款发票已创建: {} (id={})", invoice.getInvoiceNo(), invoice.getId());
        return ArInvoiceVO.from(invoice);
    }

    /**
     * 更新应收账款发票（仅 DRAFT 状态可编辑）。
     */
    @Override
    public ArInvoiceVO update(Long id, ArInvoiceUpdateDTO dto) {
        ArInvoice invoice = EntityUtil.require(id, "AR invoice", invoiceMapper::selectById);
        if (!"DRAFT".equals(invoice.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Only draft invoices can be edited");
        }

        String username = SecurityUtils.currentUsername();

        if (StringUtils.hasText(dto.getInvoiceNo())) {
            // 校验新单号唯一
            Long exists = invoiceMapper.selectCount(new LambdaQueryWrapper<ArInvoice>()
                    .eq(ArInvoice::getInvoiceNo, dto.getInvoiceNo())
                    .ne(ArInvoice::getId, id));
            if (exists > 0) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Invoice number already exists");
            }
            invoice.setInvoiceNo(dto.getInvoiceNo());
        }
        if (StringUtils.hasText(dto.getCono())) invoice.setCono(dto.getCono());
        if (StringUtils.hasText(dto.getCustomerCode())) invoice.setCustomerCode(dto.getCustomerCode());
        if (StringUtils.hasText(dto.getCustomerName())) invoice.setCustomerName(dto.getCustomerName());
        if (dto.getInvoiceDate() != null) invoice.setInvoiceDate(dto.getInvoiceDate());
        if (dto.getDueDate() != null) invoice.setDueDate(dto.getDueDate());
        if (StringUtils.hasText(dto.getOriginPoNo())) invoice.setOriginPoNo(dto.getOriginPoNo());
        if (StringUtils.hasText(dto.getOriginSoNo())) invoice.setOriginSoNo(dto.getOriginSoNo());
        if (dto.getSubtotal() != null) invoice.setSubtotal(dto.getSubtotal());
        if (dto.getTaxRate() != null) invoice.setTaxRate(dto.getTaxRate());
        if (dto.getTaxAmount() != null) invoice.setTaxAmount(dto.getTaxAmount());
        if (dto.getTotalAmount() != null) {
            invoice.setTotalAmount(dto.getTotalAmount());
            invoice.setBalance(dto.getTotalAmount().subtract(invoice.getPaidAmount()));
        }
        if (StringUtils.hasText(dto.getCurrency())) invoice.setCurrency(dto.getCurrency());
        if (dto.getNotes() != null) invoice.setNotes(dto.getNotes());

        invoice.setUpdatedBy(username);
        invoice.setUpdatedTime(LocalDateTime.now());

        // 重新计算账龄区间
        updateAgingBucket(invoice);
        invoiceMapper.updateById(invoice);

        log.info("应收账款发票已更新: {} (id={})", invoice.getInvoiceNo(), id);
        return ArInvoiceVO.from(invoice);
    }

    /**
     * 删除应收账款发票（仅 DRAFT 可删除）。
     */
    @Override
    public void delete(Long id) {
        ArInvoice invoice = EntityUtil.require(id, "AR invoice", invoiceMapper::selectById);
        if (!"DRAFT".equals(invoice.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Only draft invoices can be deleted");
        }

        // 级联删除收款记录
        paymentMapper.delete(new LambdaQueryWrapper<ArPayment>()
                .eq(ArPayment::getInvoiceId, id));
        invoiceMapper.deleteById(id);

        log.info("应收账款发票已删除: {} (id={})", invoice.getInvoiceNo(), id);
    }

    /**
     * 提交发票（DRAFT → OPEN）。
     */
    @Override
    public void submit(Long id) {
        ArInvoice invoice = EntityUtil.require(id, "AR invoice", invoiceMapper::selectById);
        if (!"DRAFT".equals(invoice.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Only draft invoices can be submitted");
        }

        invoice.setStatus("OPEN");
        invoice.setUpdatedBy(SecurityUtils.currentUsername());
        invoice.setUpdatedTime(LocalDateTime.now());
        invoiceMapper.updateById(invoice);

        log.info("应收账款发票已提交: {} (id={})", invoice.getInvoiceNo(), id);
    }

    /**
     * 记录收款（核销）。
     */
    @Override
    public ArPaymentVO receivePayment(ArPaymentCreateDTO dto) {
        ArInvoice invoice = EntityUtil.require(dto.getInvoiceId(), "AR invoice", invoiceMapper::selectById);
        if (!"OPEN".equals(invoice.getStatus()) && !"PARTIAL".equals(invoice.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Current status does not allow payment");
        }
        if (dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Payment amount must be greater than 0");
        }
        if (dto.getAmount().compareTo(invoice.getBalance()) > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Payment amount cannot exceed outstanding balance");
        }

        String username = SecurityUtils.currentUsername();

        // 生成收款单号
        String paymentNo = dto.getPaymentNo();
        if (!StringUtils.hasText(paymentNo)) {
            paymentNo = generatePaymentNo();
        }

        // 创建收款记录
        ArPayment payment = new ArPayment();
        payment.setPaymentNo(paymentNo);
        payment.setCono(dto.getCono() != null ? dto.getCono() : "001");
        payment.setInvoiceId(dto.getInvoiceId());
        payment.setPaymentDate(dto.getPaymentDate());
        payment.setAmount(dto.getAmount());
        payment.setPaymentMethod(dto.getPaymentMethod());
        payment.setReferenceNo(dto.getReferenceNo());
        payment.setReceivedBy(dto.getReceivedBy());
        payment.setNotes(dto.getNotes());
        payment.setCreatedBy(username);
        payment.setCreatedTime(LocalDateTime.now());
        paymentMapper.insert(payment);

        // 更新发票已付金额和余额
        invoice.setPaidAmount(invoice.getPaidAmount().add(dto.getAmount()));
        invoice.setBalance(invoice.getTotalAmount().subtract(invoice.getPaidAmount()));

        // 更新状态
        if (invoice.getBalance().compareTo(BigDecimal.ZERO) == 0) {
            invoice.setStatus("PAID");
        } else {
            invoice.setStatus("PARTIAL");
        }

        invoice.setUpdatedBy(username);
        invoice.setUpdatedTime(LocalDateTime.now());
        invoiceMapper.updateById(invoice);

        log.info("收款记录已创建: {} (id={})", payment.getPaymentNo(), payment.getId());
        return ArPaymentVO.from(payment);
    }

    /**
     * 生成下一个账单号（AR + yyyyMMdd + 4位序号）。
     */
    @Override
    public String generateInvoiceNo() {
        String prefix = "AR" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<ArInvoice> wrapper = new LambdaQueryWrapper<ArInvoice>()
                .likeRight(ArInvoice::getInvoiceNo, prefix)
                .orderByDesc(ArInvoice::getInvoiceNo);
        ArInvoice last = invoiceMapper.selectOne(wrapper);
        int seq = 1;
        if (last != null && last.getInvoiceNo() != null && last.getInvoiceNo().length() > prefix.length()) {
            String suffix = last.getInvoiceNo().substring(prefix.length());
            try {
                seq = Integer.parseInt(suffix) + 1;
            } catch (NumberFormatException e) {
                log.trace("Failed to parse sequence number, using default: {}", e.getMessage());
            }
        }
        return prefix + String.format("%04d", seq);
    }

    /**
     * 生成下一个收款单号（PMT + yyyyMMdd + 4位序号）。
     */
    @Override
    public String generatePaymentNo() {
        String prefix = "PMT" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<ArPayment> wrapper = new LambdaQueryWrapper<ArPayment>()
                .likeRight(ArPayment::getPaymentNo, prefix)
                .orderByDesc(ArPayment::getPaymentNo);
        ArPayment last = paymentMapper.selectOne(wrapper);
        int seq = 1;
        if (last != null && last.getPaymentNo() != null && last.getPaymentNo().length() > prefix.length()) {
            String suffix = last.getPaymentNo().substring(prefix.length());
            try {
                seq = Integer.parseInt(suffix) + 1;
            } catch (NumberFormatException e) {
                log.trace("Failed to parse sequence number, using default: {}", e.getMessage());
            }
        }
        return prefix + String.format("%04d", seq);
    }

    // ==================== 内部方法 ====================

    private ArInvoiceDetailVO toDetailVO(ArInvoice invoice) {
        ArInvoiceDetailVO vo = new ArInvoiceDetailVO();
        vo.setId(invoice.getId());
        vo.setInvoiceNo(invoice.getInvoiceNo());
        vo.setCono(invoice.getCono());
        vo.setCustomerCode(invoice.getCustomerCode());
        vo.setCustomerName(invoice.getCustomerName());
        vo.setInvoiceDate(invoice.getInvoiceDate());
        vo.setDueDate(invoice.getDueDate());
        vo.setOriginPoNo(invoice.getOriginPoNo());
        vo.setOriginSoNo(invoice.getOriginSoNo());
        vo.setSubtotal(invoice.getSubtotal());
        vo.setTaxRate(invoice.getTaxRate());
        vo.setTaxAmount(invoice.getTaxAmount());
        vo.setTotalAmount(invoice.getTotalAmount());
        vo.setPaidAmount(invoice.getPaidAmount());
        vo.setBalance(invoice.getBalance());
        vo.setCurrency(invoice.getCurrency());
        vo.setStatus(invoice.getStatus());
        vo.setStatusKey("ar.status" + invoice.getStatus());
        vo.setAgingBucket(invoice.getAgingBucket());
        vo.setNotes(invoice.getNotes());
        vo.setCreatedBy(invoice.getCreatedBy());
        vo.setCreatedTime(invoice.getCreatedTime());
        vo.setUpdatedBy(invoice.getUpdatedBy());
        vo.setUpdatedTime(invoice.getUpdatedTime());

        // 收款记录
        List<ArPayment> payments = paymentMapper.selectList(
                new LambdaQueryWrapper<ArPayment>()
                        .eq(ArPayment::getInvoiceId, invoice.getId())
                        .orderByAsc(ArPayment::getPaymentDate));
        vo.setPayments(payments.stream().map(ArPaymentVO::from).toList());

        return vo;
    }

    /**
     * 计算账龄区间。
     */
    private void updateAgingBucket(ArInvoice invoice) {
        if (invoice.getDueDate() == null || invoice.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
            invoice.setAgingBucket(null);
            return;
        }

        LocalDate today = LocalDate.now();
        long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(invoice.getDueDate(), today);

        if (daysOverdue <= 0) {
            invoice.setAgingBucket(null); // 未到期
        } else if (daysOverdue <= 30) {
            invoice.setAgingBucket("0-30");
        } else if (daysOverdue <= 60) {
            invoice.setAgingBucket("31-60");
        } else if (daysOverdue <= 90) {
            invoice.setAgingBucket("61-90");
        } else {
            invoice.setAgingBucket("90+");
        }
    }
}