package com.rxas400adm.as400.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 作业 SLA 最近执行行。
 *
 * @param jobName      作业名（JOB_NAME）
 * @param scheduleName 调度名（SCHEDULE_NAME）
 * @param expectedSec  预期耗时秒（EXPECTED_SEC）
 * @param actualSec    实际耗时秒（ACTUAL_SEC）
 * @param status       达成状态（STATUS：OK/BREACHED）
 */
public record JobSlaExecRow(
        @JsonProperty("JOB_NAME") String jobName,
        @JsonProperty("SCHEDULE_NAME") String scheduleName,
        @JsonProperty("EXPECTED_SEC") long expectedSec,
        @JsonProperty("ACTUAL_SEC") long actualSec,
        @JsonProperty("STATUS") String status) {
}
