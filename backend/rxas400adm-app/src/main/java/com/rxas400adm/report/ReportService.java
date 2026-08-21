package com.rxas400adm.report;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.rxas400adm.as400.entity.CommandScript;
import com.rxas400adm.as400.entity.JobSchedule;
import com.rxas400adm.as400.entity.JobScheduleHistory;
import com.rxas400adm.as400.mapper.CommandScriptMapper;
import com.rxas400adm.as400.mapper.JobScheduleHistoryMapper;
import com.rxas400adm.as400.mapper.JobScheduleMapper;
import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.monitor.mapper.MetricMapper;
import com.rxas400adm.monitor.service.ICapacityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 报表引擎（3.11）：指标日报/周报、执行记录报表、容量趋势报表，\n * 支持 Excel（Apache POI XSSF）与 PDF（OpenPDF，中文尝试系统 CJK 字体，缺失回退 Helvetica）。\n */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService implements IReportService {

    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MetricMapper metricMapper;
    private final JobScheduleHistoryMapper historyMapper;
    private final JobScheduleMapper scheduleMapper;
    private final CommandScriptMapper scriptMapper;
    private final ICapacityService capacityService;

    /* ---------------- 数据准备 ---------------- */

    /** 指标聚合报表：近 N 天按 指标+日期 聚合均值/峰值/最小值（P3：GROUP BY 下推到数据库） */
    public List<Map<String, Object>> metricsRows(Long instanceId, int days) {
        int window = Math.max(1, Math.min(days, 365));
        // 数据库侧按 DATE(collect_time)+metric_name 分组聚合，只回传少量汇总行
        // （旧实现全量加载原始采样后在 Java 侧聚合，采样量随监控粒度线性增长）
        List<Map<String, Object>> aggregated = metricMapper.selectAggregatedMetrics(instanceId, LocalDateTime.now().minusDays(window));
        List<Map<String, Object>> rows = new ArrayList<>(aggregated.size());
        for (Map<String, Object> a : aggregated) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", a.get("d"));
            row.put("metric", a.get("m"));
            row.put("avg", a.get("avg"));
            row.put("max", a.get("max"));
            row.put("min", a.get("min"));
            row.put("samples", a.get("samples"));
            rows.add(row);
        }
        return rows;
    }

    /** 执行记录报表：调度历史 + 脚本最近执行（与执行审计页同构） */
    public List<Map<String, Object>> executionRows(String type, String status) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (type == null || type.isBlank() || "SCHEDULE".equalsIgnoreCase(type)) {
            LambdaQueryWrapper<JobScheduleHistory> wrapper = new LambdaQueryWrapper<JobScheduleHistory>()
                    .orderByDesc(JobScheduleHistory::getRunTime)
                    .last(PageConstants.limitClause(500));
            if (status != null && !status.isBlank()) {
                wrapper.eq(JobScheduleHistory::getStatus, status.trim().toUpperCase());
            }
            // N5：先收集 history 与 scheduleId，再批量查调度定义，避免逐行 selectById 的 N+1
            List<JobScheduleHistory> histories = historyMapper.selectList(wrapper);
            List<Long> scheduleIds = histories.stream().map(JobScheduleHistory::getScheduleId).toList();
            Map<Long, JobSchedule> scheduleMap = scheduleIds.isEmpty() ? Map.of()
                    : scheduleMapper.selectBatchIds(scheduleIds).stream()
                            .collect(java.util.stream.Collectors.toMap(JobSchedule::getId, s -> s));
            for (JobScheduleHistory h : histories) {
                JobSchedule schedule = scheduleMap.get(h.getScheduleId());
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("time", h.getRunTime() == null ? "" : h.getRunTime().format(DATETIME_FMT));
                row.put("source", "SCHEDULE");
                row.put("name", schedule == null ? "#" + h.getScheduleId() : schedule.getName());
                row.put("type", schedule == null ? "?" : schedule.getScheduleType());
                row.put("user", schedule == null ? null : schedule.getCreatedBy());
                row.put("server", schedule == null ? null : schedule.getServerId());
                row.put("status", h.getStatus());
                row.put("message", h.getMessage());
                row.put("costMs", h.getCostMs());
                rows.add(row);
            }
        }
        if (type == null || type.isBlank() || "SCRIPT".equalsIgnoreCase(type)) {
            LambdaQueryWrapper<CommandScript> wrapper = new LambdaQueryWrapper<CommandScript>()
                    .isNotNull(CommandScript::getLastRunTime)
                    .orderByDesc(CommandScript::getLastRunTime)
                    .last(PageConstants.limitClause(200));
            for (CommandScript s : scriptMapper.selectList(wrapper)) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("time", s.getLastRunTime() == null ? "" : s.getLastRunTime().format(DATETIME_FMT));
                row.put("source", "SCRIPT");
                row.put("name", s.getName());
                row.put("type", "CL");
                row.put("user", s.getCreatedBy());
                row.put("server", null);
                row.put("status", "SUCCESS".equalsIgnoreCase(s.getLastRunStatus()) ? "SUCCESS" : "FAILED");
                row.put("message", s.getLastResult());
                row.put("costMs", null);
                rows.add(row);
            }
        }
        return rows;
    }

    /** 容量趋势报表：历史日均/峰值 + 未来 30 天预测 */
    public List<Map<String, Object>> capacityRows(Long instanceId, int days) {
        Map<String, Object> trend = capacityService.trend(instanceId, days);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object p : (List<?>) trend.get("points")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> point = (Map<String, Object>) p;
            Map<String, Object> row = new LinkedHashMap<>(point);
            row.put("kind", "历史");
            rows.add(row);
        }
        for (Object p : (List<?>) trend.get("prediction")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> point = (Map<String, Object>) p;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", point.get("date"));
            row.put("avg", point.get("value"));
            row.put("max", "-");
            row.put("kind", "预测");
            rows.add(row);
        }
        return rows;
    }

    /** 按报表类型统一生成（定时任务与手动导出共用）：返回渲染后的字节，失败返回空数组 */
    public byte[] generate(String reportType, String format, Long serverId, int days) {
        return switch (reportType == null ? "" : reportType.toLowerCase()) {
            case "metrics" -> {
                String title = "IBM i 指标报表（instance=" + serverId + " 近" + days + "天）";
                yield render(format, title,
                        new String[]{"date", "metric", "avg", "max", "min", "samples"},
                        metricsRows(serverId == null ? 1 : serverId, days));
            }
            case "capacity" -> {
                String title = "磁盘容量趋势报表（instance=" + serverId + "）";
                yield render(format, title,
                        new String[]{"kind", "date", "avg", "max"},
                        capacityRows(serverId == null ? 1 : serverId, days));
            }
            default -> {
                String title = "执行记录报表";
                yield render(format, title,
                        new String[]{"time", "source", "name", "type", "user", "server", "status", "message", "costMs"},
                        executionRows(null, null));
            }
        };
    }

    /* ---------------- 渲染 ---------------- */

    public byte[] render(String format, String title, String[] headers, List<Map<String, Object>> rows) {
        if ("pdf".equalsIgnoreCase(format)) {
            return renderPdf(title, headers, rows);
        }
        return renderExcel(title, headers, rows);
    }

    private byte[] renderExcel(String title, String[] headers, List<Map<String, Object>> rows) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(title.replaceAll("[\\\\/:*?\"<>|]", "_"));
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            int r = 1;
            for (Map<String, Object> row : rows) {
                Row xRow = sheet.createRow(r++);
                for (int i = 0; i < headers.length; i++) {
                    Object value = row.get(headers[i]);
                    if (value instanceof Number number) {
                        xRow.createCell(i).setCellValue(number.doubleValue());
                    } else {
                        xRow.createCell(i).setCellValue(value == null ? "" : String.valueOf(value));
                    }
                }
            }
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.warn("Excel 报表生成失败: {}", e.getMessage());
            return new byte[0];
        }
    }

    private byte[] renderPdf(String title, String[] headers, List<Map<String, Object>> rows) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();
            BaseFont baseFont = cjkBaseFont();
            if (baseFont == null) {
                return new byte[0]; // 字体不可用，放弃 PDF 渲染
            }
            Font titleFont = new Font(baseFont, 16, com.lowagie.text.Font.BOLD);
            Font cellFont = new Font(baseFont, 9, com.lowagie.text.Font.NORMAL);
            document.add(new Paragraph(title, titleFont));
            PdfPTable table = new PdfPTable(headers.length);
            table.setWidthPercentage(100);
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, new Font(baseFont, 9, com.lowagie.text.Font.BOLD)));
                cell.setPadding(3);
                table.addCell(cell);
            }
            for (Map<String, Object> row : rows) {
                for (String header : headers) {
                    Object value = row.get(header);
                    table.addCell(new Phrase(value == null ? "" : String.valueOf(value), cellFont));
                }
            }
            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.warn("PDF 报表生成失败: {}", e.getMessage());
            return new byte[0];
        }
    }

    /** 优先使用系统 CJK 字体（保证中文可读），缺失则回退 Helvetica（中文显示为框，仅数字/英文可读） */
    private BaseFont cjkBaseFont() {
        String[] candidates = {
                "C:/Windows/Fonts/msyh.ttc", "C:/Windows/Fonts/simsun.ttc",
                "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
                "/usr/share/fonts/truetype/arphic/uming.ttc"
        };
        for (String path : candidates) {
            if (Files.exists(Path.of(path))) {
                try {
                    return BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                } catch (Exception e) {
                    try {
                        return BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);
                    } catch (Exception ignored) {
                        // continue
                    }
                }
            }
        }
        try {
            return BaseFont.createFont(); // Helvetica
        } catch (Exception e) {
            return null;
        }
    }

}