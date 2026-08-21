package com.rxas400adm.report;

import java.util.List;
import java.util.Map;

public interface IReportService {

    List<Map<String, Object>> metricsRows(Long instanceId, int days);

    List<Map<String, Object>> executionRows(String type, String status);

    List<Map<String, Object>> capacityRows(Long instanceId, int days);

    byte[] generate(String reportType, String format, Long serverId, int days);

    byte[] render(String format, String title, String[] headers, List<Map<String, Object>> rows);
}