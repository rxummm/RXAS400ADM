package com.rxas400adm.monitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rxas400adm.common.constants.SqlDialect;
import com.rxas400adm.common.constants.SqlDialectHolder;
import com.rxas400adm.monitor.domain.Metric;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * SQL 已迁至 resources/mapper/MetricMapper.xml
 */
public interface MetricMapper extends BaseMapper<Metric> {

    /**
     * H2：表级分布式锁（替代 MySQL GET_LOCK，兼容 DB2 for i）。原子 UPDATE 行影响数 = 1 表示抢锁成功。
     * SQL 见 resources/mapper/MetricMapper.xml。
     */
    int tryAcquireLock(@Param("lockKey") String lockKey,
                       @Param("holder") String holder,
                       @Param("expiresAt") LocalDateTime expiresAt);

    /** H2：释放分布式锁（仅持有者本人可释放）。 */
    int releaseLock(@Param("lockKey") String lockKey, @Param("holder") String holder);

    /**
     * 报表聚合（P3：GROUP BY 下推）：近 N 天按 日期+指标 分组计算均值/峰值/最小值/采样数，
     * 替代「全量加载原始采样后 Java 侧聚合」，网络与堆内存占用与采样量无关。
     * 返回行结构：{d: 日期, m: 指标名, avg, max, min, samples}，与 ReportService 旧实现输出语义一致。
     * SQL 已迁至 resources/mapper/MetricMapper.xml（复杂聚合可读性/可直接拷库调试）。
     */
    List<Map<String, Object>> selectAggregatedMetrics(@Param("instanceId") Long instanceId,
                                                      @Param("since") LocalDateTime since);

    /**
     * 基线聚合（P3：GROUP BY 下推）：近 N 天按 指标名 分组计算均值/峰值/最小值/采样数，
     * 替代「全量加载原始采样后 Java 侧分组聚合」，网络与堆内存占用与采样量无关。
     * 返回行结构：{metricName, avgValue, maxValue, minValue, sampleCount}，与 BaselineService 旧实现输出语义一致。
     * SQL 已迁至 resources/mapper/MetricMapper.xml。
     */
    List<Map<String, Object>> selectBaselineAggregates(@Param("instanceId") Long instanceId,
                                                       @Param("since") LocalDateTime since);

    /**
     * 容量趋势聚合（P3：GROUP BY 下推）：近 N 天 DISK 指标按 天 分组计算日均/峰值，
     * 替代「全量加载 DISK 原始采样后 Java 侧按天聚合」，网络与堆内存占用与采样量无关。
     * 返回行结构：{day: LocalDate, avgValue, maxValue}，按天升序。
     * SQL 已迁至 resources/mapper/MetricMapper.xml。
     */
    List<Map<String, Object>> selectDailyDiskAggregates(@Param("instanceId") Long instanceId,
                                                        @Param("since") LocalDateTime since);

    /**
     * Overview 批量查询：一次取出 5 项指标（CPU/MEMORY/DISK/MSGW/LCKW）的最新值，
     * 替代 5 次独立 SELECT * ORDER BY collect_time DESC LIMIT 1。
     */
    List<Map<String, Object>> selectLatestOverview(@Param("instanceId") Long instanceId);

    /**
     * P5：批量入库多值 INSERT（列对照 rx_metric 表结构，id 自增不写）。
     * SQL 见 resources/mapper/MetricMapper.xml。
     */
    int insertBatch(@Param("list") List<Metric> list);

    /**
     * P18：分批删除入口——按当前数据源方言分支（SqlDialectHolder 启动时注入）：
     * MySQL 走 LIMIT 单批删除（调用方循环直至影响行数为 0）；
     * 其余方言（DB2 for i 无 LIMIT DELETE 语法）退回一次性整删，量大时依赖既有的整删窗口。
     */
    default int deleteByCreatedTimeBefore(LocalDateTime cutoff, int limit) {
        if (SqlDialectHolder.get() == SqlDialect.MYSQL) {
            return deleteBeforeLimit(cutoff, limit);
        }
        return deleteBeforeAll(cutoff);
    }

    /** P18：MySQL 方言单批删除（LIMIT 控制单批行数，避免大事务长锁）。SQL 见 XML。 */
    int deleteBeforeLimit(@Param("cutoff") LocalDateTime cutoff, @Param("limit") int limit);

    /** P18：非 MySQL 方言回退：一次性整删。SQL 见 XML。 */
    int deleteBeforeAll(@Param("cutoff") LocalDateTime cutoff);
}