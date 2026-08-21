package com.rxas400adm.as400.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * QSYS2 SQL 查询请求：仅允许 SELECT（只读查询，拒绝 DML/DDL）。
 */
@Data
public class QueryRequest {

    @NotBlank(message = "{validation.notBlank}")
    private String sql;
}
