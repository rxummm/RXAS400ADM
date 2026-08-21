package com.rxas400adm.as400.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 子系统行（QSYS2.SUBSYSTEM_INFO）。
 *
 * @param name        子系统名（SUBSYSTEM_NAME）
 * @param description 描述（SUBSYSTEM_DESCRIPTION）
 * @param status      状态（STATUS）
 * @param activeJobs  活跃作业数（NUMBER_OF_ACTIVE_JOBS）
 * @param maxJobs     最大活跃作业数（MAXIMUM_ACTIVE_JOBS）
 * @param library     所属库（SUBSYSTEM_LIBRARY）
 */
public record SubsystemRow(
        @JsonProperty("SUBSYSTEM_NAME") String name,
        @JsonProperty("SUBSYSTEM_DESCRIPTION") String description,
        @JsonProperty("STATUS") String status,
        @JsonProperty("NUMBER_OF_ACTIVE_JOBS") long activeJobs,
        @JsonProperty("MAXIMUM_ACTIVE_JOBS") long maxJobs,
        @JsonProperty("SUBSYSTEM_LIBRARY") String library) {
}
