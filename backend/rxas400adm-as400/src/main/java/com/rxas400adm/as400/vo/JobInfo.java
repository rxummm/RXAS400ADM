package com.rxas400adm.as400.vo;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 活动作业（ACTIVE_JOB_INFO）返回结构。
 * 兼容 Mock 列名（JOB_PROGRAM/CPU_TIME）与真实 QSYS2 列名（ELAPSED_CPU_PERCENTAGE）。
 */
@Data
@Builder
public class JobInfo {

    private String jobName;

    private String jobUser;

    private String jobNumber;

    private String jobStatus;

    private String jobProgram;

    private String cpuTime;

    private String temporaryStorage;

    public static JobInfo from(Map<String, Object> row) {
        return JobInfo.builder()
                .jobName(str(row.get("JOB_NAME")))
                .jobUser(str(row.get("JOB_USER")))
                .jobNumber(str(row.get("JOB_NUMBER")))
                .jobStatus(str(row.get("JOB_STATUS")))
                .jobProgram(str(row.getOrDefault("JOB_PROGRAM", row.get("ELAPSED_CPU_PERCENTAGE") == null ? "" : "QSYS")))
                .cpuTime(str(row.getOrDefault("CPU_TIME", row.get("ELAPSED_CPU_PERCENTAGE"))))
                .temporaryStorage(str(row.get("TEMPORARY_STORAGE")))
                .build();
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }
}
