package com.rxas400adm.as400.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 作业队列行（QSYS2.JOB_QUEUE_INFO）。
 *
 * @param name        队列名（JOB_QUEUE_NAME）
 * @param library     队列库（JOB_QUEUE_LIBRARY）
 * @param status      队列状态（JOB_QUEUE_STATUS）
 * @param numJobs     队列中作业数（NUMBER_OF_JOBS）
 * @param type        队列类型（JOB_QUEUE_TYPE）
 */
public record JobQueueRow(
        @JsonProperty("JOB_QUEUE_NAME") String name,
        @JsonProperty("JOB_QUEUE_LIBRARY") String library,
        @JsonProperty("JOB_QUEUE_STATUS") String status,
        @JsonProperty("NUMBER_OF_JOBS") long numJobs,
        @JsonProperty("JOB_QUEUE_TYPE") String type) {
}
