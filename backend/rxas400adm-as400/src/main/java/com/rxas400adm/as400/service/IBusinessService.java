package com.rxas400adm.as400.service;

import java.util.List;
import java.util.Map;

/**
 * 通用业务数据查询：任意 库.表 → 字段定义 + 分页数据 + 关键词模糊查询。
 * 数据源按 X-AS400-Server 头路由当前服务器。
 */
public interface IBusinessService {

    List<Map<String, Object>> tables(String library, String keyword);

    List<Map<String, Object>> columns(String library, String file);

    Map<String, Object> data(String library, String file, String keyword, int page, int size);
}
