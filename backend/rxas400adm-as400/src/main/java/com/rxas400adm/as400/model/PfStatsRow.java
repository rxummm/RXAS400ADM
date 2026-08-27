package com.rxas400adm.as400.model;

import java.util.List;

/**
 * PF 物理文件统计信息（记录数 / 存储 / 索引）。
 *
 * @param recordCount   记录数
 * @param storageSize   存储大小（字节）
 * @param indexCount    索引数量
 * @param indexNames    索引名称列表
 * @param memberCount   成员数
 */
public record PfStatsRow(
        long recordCount,
        long storageSize,
        int indexCount,
        List<String> indexNames,
        int memberCount) {
}
