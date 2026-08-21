package com.rxas400adm.as400.vo;

import com.rxas400adm.as400.entity.JobSla;

/**
 * 作业 SLA 视图：与 JobSla 字段契约解耦。
 */
public record JobSlaVO(
        Long id,
        String jobName,
        String scheduleName,
        Integer expectedDurationSec,
        Integer deviationPercent,
        Boolean enabled) {

    public static JobSlaVO from(JobSla e) {
        return new JobSlaVO(
                e.getId(), e.getJobName(), e.getScheduleName(),
                e.getExpectedDurationSec(), e.getDeviationPercent(), e.getEnabled());
    }
}