package com.rxas400adm.as400.service;

import com.rxas400adm.as400.vo.BpcsForecastVO;

import java.util.List;
import java.util.Map;

/**
 * ⑥ 预测补货看板接口。
 */
public interface IBpcsForecastService {

    /** 获取预测数据（含月度需求、库存水平、补货建议、准确度指标） */
    BpcsForecastVO getForecast(String cono, String item, int months);

    /** 获取物料选项列表（用于下拉框） */
    List<Map<String, String>> getItemOptions(String cono, int limit);
}
