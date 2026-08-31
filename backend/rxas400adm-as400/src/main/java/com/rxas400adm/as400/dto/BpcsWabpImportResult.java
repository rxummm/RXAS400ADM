package com.rxas400adm.as400.dto;

import lombok.Data;

import java.util.List;

@Data
public class BpcsWabpImportResult {
    private int successCount;
    private int failureCount;
    private List<String> errors;
}