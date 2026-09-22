package com.rxas400adm.procurement.vo;

import com.rxas400adm.procurement.entity.PurchaseOrderItem;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 采购订单行项 VO。
 */
@Data
public class PurchaseOrderItemVO {

    private Long id;
    private Long poId;
    private Integer lineNo;
    private String itemCode;
    private String itemDesc;
    private String uom;
    private BigDecimal qtyOrdered;
    private BigDecimal qtyReceived;
    private BigDecimal qtyInvoiced;
    private BigDecimal unitPrice;
    private BigDecimal lineAmount;
    private LocalDate reqDate;
    private LocalDate receivedDate;
    private String notes;

    public static PurchaseOrderItemVO from(PurchaseOrderItem entity) {
        PurchaseOrderItemVO vo = new PurchaseOrderItemVO();
        vo.setId(entity.getId());
        vo.setPoId(entity.getPoId());
        vo.setLineNo(entity.getLineNo());
        vo.setItemCode(entity.getItemCode());
        vo.setItemDesc(entity.getItemDesc());
        vo.setUom(entity.getUom());
        vo.setQtyOrdered(entity.getQtyOrdered());
        vo.setQtyReceived(entity.getQtyReceived());
        vo.setQtyInvoiced(entity.getQtyInvoiced());
        vo.setUnitPrice(entity.getUnitPrice());
        vo.setLineAmount(entity.getLineAmount());
        vo.setReqDate(entity.getReqDate());
        vo.setReceivedDate(entity.getReceivedDate());
        vo.setNotes(entity.getNotes());
        return vo;
    }
}