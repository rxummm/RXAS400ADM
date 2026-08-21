package com.rxas400adm.report.vo;

import java.util.List;
import java.util.Map;

/**
 * 报表预览结果（ReportController.preview 返回）。
 */
public record ReportPreviewVO(long rows, Map<String, Object> sample) {
}
