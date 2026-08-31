package com.rxas400adm.as400.vo;

import java.util.List;

/**
 * 库存多级一致性核对结果 VO。
 */
public record BpcsInventoryConsistencyVO(
        String item,
        /** Pallet 级汇总 */
        LevelSummary palletLevel,
        /** 库位级汇总 */
        LevelSummary locationLevel,
        /** 仓库级汇总 */
        LevelSummary warehouseLevel,
        /** 是否一致 */
        boolean consistent,
        /** 差异描述（不一致时） */
        String discrepancyNote
) {
    /** 单级汇总 */
    public record LevelSummary(
            String levelName,
            int totalQuantity,
        int detailCount,
        boolean matched,
            /** 明细行（仅查详情时填充） */
            List<LevelDetail> details
    ) {}

    /** 明细行 */
    public record LevelDetail(
            String identifier,
            int quantity,
            String extraInfo
    ) {}
}
