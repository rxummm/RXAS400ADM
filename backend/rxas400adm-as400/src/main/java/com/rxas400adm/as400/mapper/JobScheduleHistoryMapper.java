package com.rxas400adm.as400.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rxas400adm.as400.entity.JobScheduleHistory;

import java.util.Map;

public interface JobScheduleHistoryMapper extends BaseMapper<JobScheduleHistory> {

    /**
     * P16c 统计下推：单条聚合 SQL 直出 调度历史+命令脚本 两表执行统计
     * （COUNT / SUM(CASE) / AVG 下推 DB，替代内存聚合），返回单行 Map：
     * totalCnt 总次数、successCnt 成功数、avgCost 平均耗时 ms（空表为 NULL）。
     */
    Map<String, Object> selectExecutionStats();
}
