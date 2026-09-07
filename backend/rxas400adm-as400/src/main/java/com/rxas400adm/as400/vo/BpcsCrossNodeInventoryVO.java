package com.rxas400adm.as400.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 跨节点库存可视化 VO。
 * 展示多仓库/工厂间的库存分布、流转和平衡状态。
 */
public record BpcsCrossNodeInventoryVO(
        List<NodeInfo> nodes,
        List<NodeFlow> flows,
        List<ImbalanceItem> imbalances,
        InventoryHeatmap heatmap
) {
    /** 仓库/工厂节点信息 */
    public record NodeInfo(
            String nodeId,
            String nodeName,
            String nodeType,
            int totalItems,
            int totalOnHand,
            BigDecimal totalValue,
            double capacityPct,
            int alertCount
    ) {}

    /** 节点间流转 */
    public record NodeFlow(
            String fromNode,
            String toNode,
            String item,
            int quantity,
            String flowType,
            String plannedDate
    ) {}

    /** 库存失衡项 */
    public record ImbalanceItem(
            String item,
            String itemDesc,
            List<WhStock> warehouseStocks,
            double imbalanceIndex,
            String recommendation
    ) {}

    /** 仓库库存明细 */
    public record WhStock(
            String warehouse,
            int onHand,
            int allocated,
            int available,
            int safetyStock
    ) {}

    /** 库存热力图数据（物料 × 仓库） */
    public record InventoryHeatmap(
            List<String> items,
            List<String> warehouses,
            List<List<Integer>> data
    ) {}
}
