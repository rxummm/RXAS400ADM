package com.rxas400adm.report;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * 报表渲染器（中-3 从 ReportService 拆分）：纯技术渲染，零业务语义。
 * Excel 走 Apache POI XSSF，PDF 走 OpenPDF（中文尝试系统 CJK 字体，缺失回退 Helvetica）。
 * 无状态静态工具；标题/表头的业务定义统一收敛在 {@link ReportSpec}。
 */
@Slf4j
final class ReportRenderer {

    private ReportRenderer() {
    }

    static byte[] render(String format, String title, String[] headers, List<Map<String, Object>> rows) {
        if ("pdf".equalsIgnoreCase(format)) {
            return renderPdf(title, headers, rows);
        }
        return renderExcel(title, headers, rows);
    }

    private static byte[] renderExcel(String title, String[] headers, List<Map<String, Object>> rows) {
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

    private static byte[] renderPdf(String title, String[] headers, List<Map<String, Object>> rows) {
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
    private static BaseFont cjkBaseFont() {
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
