package com.rxas400adm.as400.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rxas400adm.as400.AS400Client;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.dto.JobSlaDTO;
import com.rxas400adm.as400.entity.JobSla;
import com.rxas400adm.as400.mapper.JobSlaMapper;
import com.rxas400adm.as400.model.JobSlaExecRow;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 作业 SLA（借鉴旧项目 jobSla）：本地维护关键作业预期耗时规则，
 * 最近执行数据取自 AS400Client.jobSlaExecutions()（Mock 仿真 / JT400 真实作业历史）。
 */
@Service
@RequiredArgsConstructor
public class JobSlaService implements IJobSlaService {

    private final JobSlaMapper slaMapper;
    private final AS400ClientProvider clientProvider;

    /** P1-7：聚合查询缓存名（与 TopologyService/JobDependencyService 共用，60s TTL） */
    private static final String CACHE = "as400Aggregate";

    public List<JobSla> list() {
        return slaMapper.selectList(new LambdaQueryWrapper<JobSla>()
                .orderByDesc(JobSla::getCreatedTime));
    }

    @CacheEvict(cacheNames = CACHE, allEntries = true)
    public JobSla create(JobSlaDTO dto, String username) {
        JobSla sla = new JobSla();
        sla.setJobName(dto.getJobName());
        sla.setScheduleName(dto.getScheduleName());
        sla.setExpectedDurationSec(dto.getExpectedDurationSec());
        sla.setDeviationPercent(dto.getDeviationPercent());
        sla.setEnabled(dto.getEnabled());
        if (sla.getJobName() == null || sla.getJobName().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Job name is required");
        }
        if (sla.getExpectedDurationSec() == null || sla.getExpectedDurationSec() <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Expected duration must be greater than 0");
        }
        sla.setId(null);
        sla.setEnabled(sla.getEnabled() == null || sla.getEnabled());
        sla.setDeviationPercent(sla.getDeviationPercent() == null ? 20 : sla.getDeviationPercent());
        sla.setCreatedTime(LocalDateTime.now());
        sla.setUpdatedTime(LocalDateTime.now());
        slaMapper.insert(sla);
        return sla;
    }

    @CacheEvict(cacheNames = CACHE, allEntries = true)
    public JobSla update(Long id, JobSlaDTO sla) {
        JobSla existing = slaMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "SLA rule not found");
        }
        if (sla.getJobName() != null) {
            existing.setJobName(sla.getJobName());
        }
        if (sla.getScheduleName() != null) {
            existing.setScheduleName(sla.getScheduleName());
        }
        if (sla.getExpectedDurationSec() != null && sla.getExpectedDurationSec() > 0) {
            existing.setExpectedDurationSec(sla.getExpectedDurationSec());
        }
        if (sla.getDeviationPercent() != null) {
            existing.setDeviationPercent(sla.getDeviationPercent());
        }
        if (sla.getEnabled() != null) {
            existing.setEnabled(sla.getEnabled());
        }
        existing.setUpdatedTime(LocalDateTime.now());
        slaMapper.updateById(existing);
        return existing;
    }

    @CacheEvict(cacheNames = CACHE, allEntries = true)
    public void delete(Long id) {
        if (slaMapper.selectById(id) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "SLA rule not found");
        }
        slaMapper.deleteById(id);
    }

    /**
     * 最近执行对比：SLA 规则（预期） + 执行数据（实际），按作业名匹配统一计算达成状态。
     * 数据源为 AS400Client.jobSlaExecutions()（Mock 仿真 / JT400 真实作业历史，两者格式一致）。
     * 客户端返回不可变 {@link JobSlaExecRow}，此处转为可变 Map 后按规则重算
     * EXPECTED_SEC / ACTUAL_SEC / STATUS（JSON 键与前端保持一致的大写形式）。
     */
    @Cacheable(cacheNames = CACHE, keyGenerator = "serverAwareKeyGenerator")
    public List<Map<String, Object>> executions() {
        AS400Client client = clientProvider.current();
        List<JobSlaExecRow> execs = client.jobSlaExecutions();
        if (execs == null || execs.isEmpty()) {
            return List.of();
        }
        List<JobSla> rules = list();
        List<Map<String, Object>> result = new ArrayList<>();
        for (JobSlaExecRow row : execs) {
            Map<String, Object> exec = new LinkedHashMap<>();
            exec.put("JOB_NAME", row.jobName());
            exec.put("SCHEDULE_NAME", row.scheduleName());
            exec.put("EXPECTED_SEC", row.expectedSec());
            exec.put("ACTUAL_SEC", row.actualSec());
            exec.put("STATUS", row.status());
            String job = row.jobName() == null ? "" : row.jobName();
            JobSla rule = rules.stream()
                    .filter(r -> r.getJobName().equalsIgnoreCase(job))
                    .findFirst()
                    .orElse(null);
            if (rule != null) {
                long expected = rule.getExpectedDurationSec();
                double deviation = rule.getDeviationPercent() == null ? 20 : rule.getDeviationPercent();
                double threshold = expected * (1 + deviation / 100.0);
                exec.put("EXPECTED_SEC", expected);
                long actual = row.actualSec();
                if (actual > 0) {
                    exec.put("ACTUAL_SEC", actual);
                    exec.put("STATUS", actual <= threshold ? "OK" : "BREACHED");
                }
            }
            // 未命中规则且无状态（JT400 行）默认 OK
            if (exec.get("STATUS") == null || String.valueOf(exec.get("STATUS")).isBlank()) {
                exec.put("STATUS", "OK");
            }
            result.add(exec);
        }
        return List.copyOf(result);
    }
}
