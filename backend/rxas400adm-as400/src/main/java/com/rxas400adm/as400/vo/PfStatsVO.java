package com.rxas400adm.as400.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * PF 统计信息 VO（替代 Entity 返回前端）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PfStatsVO {
    private Long recordCount;
    private Long storageSize;
    private Integer indexCount;
    private List<String> indexNames;
    private Integer memberCount;
}
