package com.rxas400adm.procurement.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.common.util.EntityUtil;
import com.rxas400adm.common.util.SecurityUtils;
import com.rxas400adm.procurement.dto.PurchaseApprovalActionDTO;
import com.rxas400adm.procurement.dto.PurchaseOrderCreateDTO;
import com.rxas400adm.procurement.dto.PurchaseOrderQueryDTO;
import com.rxas400adm.procurement.dto.PurchaseOrderUpdateDTO;
import com.rxas400adm.procurement.entity.PurchaseApproval;
import com.rxas400adm.procurement.entity.PurchaseOrder;
import com.rxas400adm.procurement.entity.PurchaseOrderItem;
import com.rxas400adm.procurement.mapper.PurchaseApprovalMapper;
import com.rxas400adm.procurement.mapper.PurchaseOrderItemMapper;
import com.rxas400adm.procurement.mapper.PurchaseOrderMapper;
import com.rxas400adm.procurement.vo.PurchaseApprovalVO;
import com.rxas400adm.procurement.vo.PurchaseOrderDetailVO;
import com.rxas400adm.procurement.vo.PurchaseOrderItemVO;
import com.rxas400adm.procurement.vo.PurchaseOrderVO;
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
 * 采购订单管理服务。
 * 支持完整 CRUD、多级审批流、状态跟踪。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseOrderService {

    private static final List<String> APPROVAL_ACTIONS = List.of("APPROVED", "REJECTED", "RETURNED");

    private final PurchaseOrderMapper orderMapper;
    private final PurchaseOrderItemMapper itemMapper;
    private final PurchaseApprovalMapper approvalMapper;

    /**
     * 分页查询采购订单。
     */
    public PageResult<PurchaseOrderVO> pageQuery(PurchaseOrderQueryDTO query) {
        LambdaQueryWrapper<PurchaseOrder> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getPoNo())) {
            wrapper.like(PurchaseOrder::getPoNo, query.getPoNo());
        }
        if (StringUtils.hasText(query.getVendorCode())) {
            wrapper.eq(PurchaseOrder::getVendorCode, query.getVendorCode());
        }
        if (StringUtils.hasText(query.getVendorName())) {
            wrapper.like(PurchaseOrder::getVendorName, query.getVendorName());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(PurchaseOrder::getStatus, query.getStatus());
        }
        if (query.getOrderDateFrom() != null) {
            wrapper.ge(PurchaseOrder::getOrderDate, query.getOrderDateFrom());
        }
        if (query.getOrderDateTo() != null) {
            wrapper.le(PurchaseOrder::getOrderDate, query.getOrderDateTo());
        }
        wrapper.orderByDesc(PurchaseOrder::getCreatedTime);

        IPage<PurchaseOrder> page = orderMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<PurchaseOrderVO> records = page.getRecords().stream()
                .map(PurchaseOrderVO::from).toList();
        return new PageResult<>(page.getTotal(), records);
    }

    /**
     * 获取采购订单详情（含行项和审批历史）。
     */
    public PurchaseOrderDetailVO getDetail(Long id) {
        return toDetailVO(EntityUtil.require(id, "Purchase order", orderMapper::selectById));
    }

    /**
     * 新建采购订单（DRAFT）。
     */
    public PurchaseOrderVO create(PurchaseOrderCreateDTO dto) {
        String username = SecurityUtils.currentUsername();

        // 校验单号唯一
        Long exists = orderMapper.selectCount(new LambdaQueryWrapper<PurchaseOrder>()
                .eq(PurchaseOrder::getPoNo, dto.getPoNo()));
        if (exists > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Purchase order number already exists");
        }

        // 创建订单头
        PurchaseOrder order = new PurchaseOrder();
        order.setPoNo(dto.getPoNo());
        order.setCono(dto.getCono() != null ? dto.getCono() : "001");
        order.setVendorCode(dto.getVendorCode());
        order.setVendorName(dto.getVendorName());
        order.setOrderDate(dto.getOrderDate() != null ? dto.getOrderDate() : LocalDate.now());
        order.setReqDate(dto.getReqDate());
        order.setCurrency(dto.getCurrency() != null ? dto.getCurrency() : "CNY");
        order.setStatus("DRAFT");
        order.setApprovalLevel(0);
        order.setApprovalStatus("PENDING");
        order.setNotes(dto.getNotes());
        order.setCreatedBy(username);
        order.setCreatedTime(LocalDateTime.now());
        orderMapper.insert(order);

        // 创建行项
        createItems(order.getId(), dto.getItems());

        order.setTotalAmount(calculateTotal(order.getId()));
        orderMapper.updateById(order);

        log.info("采购订单已创建: {} (id={})", order.getPoNo(), order.getId());
        return PurchaseOrderVO.from(order);
    }

    /**
     * 更新采购订单（仅 DRAFT 状态可编辑）。
     */
    public PurchaseOrderVO update(Long id, PurchaseOrderUpdateDTO dto) {
        PurchaseOrder order = EntityUtil.require(id, "Purchase order", orderMapper::selectById);
        if (!"DRAFT".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Only draft orders can be edited");
        }

        String username = SecurityUtils.currentUsername();

        if (StringUtils.hasText(dto.getPoNo())) {
            // 校验新单号唯一
            Long exists = orderMapper.selectCount(new LambdaQueryWrapper<PurchaseOrder>()
                    .eq(PurchaseOrder::getPoNo, dto.getPoNo())
                    .ne(PurchaseOrder::getId, id));
            if (exists > 0) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Purchase order number already exists");
            }
            order.setPoNo(dto.getPoNo());
        }
        if (StringUtils.hasText(dto.getVendorCode())) order.setVendorCode(dto.getVendorCode());
        if (StringUtils.hasText(dto.getVendorName())) order.setVendorName(dto.getVendorName());
        if (dto.getOrderDate() != null) order.setOrderDate(dto.getOrderDate());
        if (dto.getReqDate() != null) order.setReqDate(dto.getReqDate());
        if (StringUtils.hasText(dto.getCurrency())) order.setCurrency(dto.getCurrency());
        if (dto.getNotes() != null) order.setNotes(dto.getNotes());
        order.setUpdatedBy(username);
        order.setUpdatedTime(LocalDateTime.now());

        // 全量替换行项
        if (dto.getItems() != null) {
            itemMapper.delete(new LambdaQueryWrapper<PurchaseOrderItem>().eq(PurchaseOrderItem::getPoId, id));
            createItems(id, dto.getItems());
        }

        order.setTotalAmount(calculateTotal(id));
        orderMapper.updateById(order);

        log.info("采购订单已更新: {} (id={})", order.getPoNo(), id);
        return PurchaseOrderVO.from(orderMapper.selectById(id));
    }

    /**
     * 提交审批（DRAFT → PENDING_APPROVAL）。
     */
    public void submitForApproval(Long id) {
        PurchaseOrder order = EntityUtil.require(id, "Purchase order", orderMapper::selectById);
        if (!"DRAFT".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Only draft orders can be submitted for approval");
        }

        order.setStatus("PENDING_APPROVAL");
        order.setApprovalLevel(1);
        order.setApprovalStatus("PENDING");
        order.setUpdatedBy(SecurityUtils.currentUsername());
        order.setUpdatedTime(LocalDateTime.now());
        orderMapper.updateById(order);

        log.info("采购订单已提交审批: {} (id={})", order.getPoNo(), id);
    }

    /**
     * 审批操作（APPROVED/REJECTED/RETURNED）。
     */
    public void handleApproval(PurchaseApprovalActionDTO dto) {
        PurchaseOrder order = EntityUtil.require(dto.getPoId(), "Purchase order", orderMapper::selectById);
        if (!"PENDING_APPROVAL".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Current status does not allow approval action");
        }
        if (!APPROVAL_ACTIONS.contains(dto.getAction())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid approval action");
        }

        String username = SecurityUtils.currentUsername();
        int currentLevel = order.getApprovalLevel() != null ? order.getApprovalLevel() : 0;

        // 记录审批操作
        PurchaseApproval approval = new PurchaseApproval();
        approval.setPoId(order.getId());
        approval.setLevel(currentLevel);
        approval.setApprover(username);
        approval.setAction(dto.getAction());
        approval.setComment(dto.getComment());
        approval.setActionTime(LocalDateTime.now());
        approvalMapper.insert(approval);

        switch (dto.getAction()) {
            case "APPROVED" -> {
                if (currentLevel >= 3) {
                    // 三级审批全部通过
                    order.setStatus("APPROVED");
                    order.setApprovalStatus("APPROVED");
                    order.setApprovedBy(username);
                    order.setApprovedTime(LocalDateTime.now());
                } else {
                    // 进入下一级审批
                    order.setApprovalLevel(currentLevel + 1);
                }
            }
            case "REJECTED" -> {
                order.setStatus("DRAFT");
                order.setApprovalStatus("REJECTED");
                order.setApprovalLevel(0);
            }
            case "RETURNED" -> {
                order.setStatus("DRAFT");
                order.setApprovalStatus("REJECTED");
                order.setApprovalLevel(0);
            }
        }

        order.setUpdatedBy(username);
        order.setUpdatedTime(LocalDateTime.now());
        orderMapper.updateById(order);

        log.info("采购订单审批: {} action={} level={} (id={})",
                order.getPoNo(), dto.getAction(), currentLevel, order.getId());
    }

    /**
     * 收货操作（APPROVED/SHIPPED → RECEIVED）。
     */
    public void receiveOrder(Long id) {
        PurchaseOrder order = EntityUtil.require(id, "Purchase order", orderMapper::selectById);
        if (!"APPROVED".equals(order.getStatus()) && !"SHIPPED".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Current status does not allow receiving action");
        }

        order.setStatus("RECEIVED");
        order.setUpdatedBy(SecurityUtils.currentUsername());
        order.setUpdatedTime(LocalDateTime.now());
        orderMapper.updateById(order);

        // 更新行项收货日期
        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setReceivedDate(LocalDate.now());
        itemMapper.update(item, new LambdaQueryWrapper<PurchaseOrderItem>()
                .eq(PurchaseOrderItem::getPoId, id));

        log.info("采购订单已收货: {} (id={})", order.getPoNo(), id);
    }

    /**
     * 取消订单（仅 DRAFT/PENDING_APPROVAL 可取消）。
     */
    public void cancelOrder(Long id) {
        PurchaseOrder order = EntityUtil.require(id, "Purchase order", orderMapper::selectById);
        if (!"DRAFT".equals(order.getStatus()) && !"PENDING_APPROVAL".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Current status does not allow cancel action");
        }

        order.setStatus("CANCELLED");
        order.setUpdatedBy(SecurityUtils.currentUsername());
        order.setUpdatedTime(LocalDateTime.now());
        orderMapper.updateById(order);

        log.info("采购订单已取消: {} (id={})", order.getPoNo(), id);
    }

    /**
     * 删除订单（仅 DRAFT 可删除）。
     */
    public void deleteOrder(Long id) {
        PurchaseOrder order = EntityUtil.require(id, "Purchase order", orderMapper::selectById);
        if (!"DRAFT".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Only draft orders can be deleted");
        }

        // 级联删除行项和审批记录（外键 ON DELETE CASCADE）
        itemMapper.delete(new LambdaQueryWrapper<PurchaseOrderItem>().eq(PurchaseOrderItem::getPoId, id));
        approvalMapper.delete(new LambdaQueryWrapper<PurchaseApproval>().eq(PurchaseApproval::getPoId, id));
        orderMapper.deleteById(id);

        log.info("采购订单已删除: {} (id={})", order.getPoNo(), id);
    }

    /**
     * 生成下一个采购单号（PO + yyyyMMdd + 4位序号）。
     */
    public String generatePoNo() {
        String prefix = "PO" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<PurchaseOrder> wrapper = new LambdaQueryWrapper<PurchaseOrder>()
                .likeRight(PurchaseOrder::getPoNo, prefix)
                .orderByDesc(PurchaseOrder::getPoNo);
        PurchaseOrder last = orderMapper.selectOne(wrapper);
        int seq = 1;
        if (last != null && last.getPoNo() != null && last.getPoNo().length() > prefix.length()) {
            String suffix = last.getPoNo().substring(prefix.length());
            try {
                seq = Integer.parseInt(suffix) + 1;
            } catch (NumberFormatException e) {
                log.trace("Failed to parse sequence number, using default: {}", e.getMessage());
            }
        }
        return prefix + String.format("%04d", seq);
    }

    // ==================== 内部方法 ====================

    private void createItems(Long poId, List<PurchaseOrderCreateDTO.ItemDTO> items) {
        int lineNo = 1;
        for (PurchaseOrderCreateDTO.ItemDTO dto : items) {
            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setPoId(poId);
            item.setLineNo(dto.getLineNo() != null ? dto.getLineNo() : lineNo);
            item.setItemCode(dto.getItemCode());
            item.setItemDesc(dto.getItemDesc());
            item.setUom(dto.getUom() != null ? dto.getUom() : "EA");
            item.setQtyOrdered(dto.getQtyOrdered() != null ? dto.getQtyOrdered() : BigDecimal.ZERO);
            item.setQtyReceived(BigDecimal.ZERO);
            item.setQtyInvoiced(BigDecimal.ZERO);
            item.setUnitPrice(dto.getUnitPrice() != null ? dto.getUnitPrice() : BigDecimal.ZERO);
            item.setLineAmount(item.getQtyOrdered().multiply(item.getUnitPrice()));
            item.setReqDate(dto.getReqDate());
            item.setNotes(dto.getNotes());
            item.setCreatedTime(LocalDateTime.now());
            itemMapper.insert(item);
            lineNo++;
        }
    }

    private BigDecimal calculateTotal(Long poId) {
        List<PurchaseOrderItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<PurchaseOrderItem>().eq(PurchaseOrderItem::getPoId, poId));
        return items.stream()
                .map(i -> i.getLineAmount() != null ? i.getLineAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private PurchaseOrderDetailVO toDetailVO(PurchaseOrder order) {
        PurchaseOrderDetailVO vo = new PurchaseOrderDetailVO();
        vo.setId(order.getId());
        vo.setPoNo(order.getPoNo());
        vo.setCono(order.getCono());
        vo.setVendorCode(order.getVendorCode());
        vo.setVendorName(order.getVendorName());
        vo.setOrderDate(order.getOrderDate());
        vo.setReqDate(order.getReqDate());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setCurrency(order.getCurrency());
        vo.setStatus(order.getStatus());
        vo.setStatusKey("procurement.po.status" + order.getStatus());
        vo.setApprovalLevel(order.getApprovalLevel());
        vo.setApprovalStatus(order.getApprovalStatus());
        vo.setApprovedBy(order.getApprovedBy());
        vo.setApprovedTime(order.getApprovedTime());
        vo.setNotes(order.getNotes());
        vo.setAs400PoNo(order.getAs400PoNo());
        vo.setCreatedBy(order.getCreatedBy());
        vo.setCreatedTime(order.getCreatedTime());
        vo.setUpdatedBy(order.getUpdatedBy());
        vo.setUpdatedTime(order.getUpdatedTime());

        // 行项
        List<PurchaseOrderItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<PurchaseOrderItem>().eq(PurchaseOrderItem::getPoId, order.getId())
                        .orderByAsc(PurchaseOrderItem::getLineNo));
        vo.setItems(items.stream().map(PurchaseOrderItemVO::from).toList());

        // 审批历史
        List<PurchaseApproval> approvals = approvalMapper.selectList(
                new LambdaQueryWrapper<PurchaseApproval>().eq(PurchaseApproval::getPoId, order.getId())
                        .orderByAsc(PurchaseApproval::getLevel).orderByAsc(PurchaseApproval::getActionTime));
        vo.setApprovals(approvals.stream().map(PurchaseApprovalVO::from).toList());

        return vo;
    }
}