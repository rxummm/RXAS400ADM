package com.rxas400adm.as400.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * SPOOL 文件行（QSYS2.OUTPUT_QUEUE_ENTRIES）。
 *
 * @param name        spool 文件名（SPOOLED_FILE_NAME）
 * @param jobName     作业名（JOB_NAME）
 * @param jobUser     作业用户（JOB_USER）
 * @param jobNumber   作业号（JOB_NUMBER）
 * @param outputQueue 输出队列（OUTPUT_QUEUE）
 * @param status      状态（SPOOLED_FILE_STATUS）
 * @param pages       页数（NUMBER_OF_PAGES）
 * @param userData    用户数据（USER_DATA）
 */
public record SpoolRow(
        @JsonProperty("SPOOLED_FILE_NAME") String name,
        @JsonProperty("JOB_NAME") String jobName,
        @JsonProperty("JOB_USER") String jobUser,
        @JsonProperty("JOB_NUMBER") String jobNumber,
        @JsonProperty("OUTPUT_QUEUE") String outputQueue,
        @JsonProperty("SPOOLED_FILE_STATUS") String status,
        @JsonProperty("NUMBER_OF_PAGES") long pages,
        @JsonProperty("USER_DATA") String userData) {
}
