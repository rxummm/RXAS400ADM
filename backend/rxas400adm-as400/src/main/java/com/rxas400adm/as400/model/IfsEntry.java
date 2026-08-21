package com.rxas400adm.as400.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * IFS 目录条目（2.3.5）。JSON 键与旧 Map 返回一致（大写），前端无感知。
 *
 * @param name     文件/目录名（NAME）
 * @param type     DIR 或 FILE（TYPE）
 * @param size     大小字节（SIZE）
 * @param modified 修改时间（MODIFIED）
 * @param path     完整路径（PATH）
 */
public record IfsEntry(
        @JsonProperty("NAME") String name,
        @JsonProperty("TYPE") String type,
        @JsonProperty("SIZE") long size,
        @JsonProperty("MODIFIED") String modified,
        @JsonProperty("PATH") String path) {
}
