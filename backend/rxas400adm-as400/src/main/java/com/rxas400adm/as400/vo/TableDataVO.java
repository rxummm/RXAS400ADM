package com.rxas400adm.as400.vo;

import java.util.List;
import java.util.Map;

/**
 * 业务数据分页查询结果（BusinessController.data 返回）。
 */
public record TableDataVO(long total, List<String> columns, List<Map<String, Object>> rows) {
}
