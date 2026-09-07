package com.rxas400adm.as400.vo;

/**
 * ㉗ 库位库存 VO。
 */
public record BpcsLocationInventoryVO(
        String warehouse,
        String binLocation,
        String item,
        String description,
        Integer qtyOnHand,
        String status
) {
}
