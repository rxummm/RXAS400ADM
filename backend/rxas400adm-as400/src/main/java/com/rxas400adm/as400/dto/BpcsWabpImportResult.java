package com.rxas400adm.as400.dto;

import lombok.Data;

import java.util.List;

@Data
public class BpcsWabpImportResult {
    private Integer successCount;
    private Integer failureCount;
    private List<String> errors;
}