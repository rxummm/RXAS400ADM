package com.rxas400adm.as400.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * QSYS2 SQL 查询请求：仅允许 SELECT（只读查询，拒绝 DML/DDL）。
 */
@Data
public class QueryRequest {

    @NotBlank(message = "{validation.notBlank}")
    @Schema(description = "SELECT SQL语句", example = "SELECT * FROM QSYS2.SYSVAL")
    private String sql;
}