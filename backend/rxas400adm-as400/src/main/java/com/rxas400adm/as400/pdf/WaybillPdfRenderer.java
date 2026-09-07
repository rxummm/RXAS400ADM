package com.rxas400adm.as400.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Table;
import com.lowagie.text.pdf.PdfWriter;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 运输单 PDF 渲染器（OpenPDF / iText 2.x 维护分支）
 * <p>
 * 生成标准 A4 运输单，包含：公司抬头、运单号、收发双方、物料明细、签收栏。
 */
@Component
public class WaybillPdfRenderer {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * 渲染运单 PDF
     *
     * @param waybillNo 运单号
     * @param params    运单数据（shipFrom/shipTo/items/notes 等）
     * @return PDF 字节数组
     */
    public byte[] render(String waybillNo, Map<String, Object> params) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 36, 36, 50, 40);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            // ---------- 标题 ----------
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Paragraph title = new Paragraph("Transport Waybill / 运输单", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(6);
            doc.add(title);

            // ---------- 基本信息表 ----------
            Table infoTable = new Table(4);
            infoTable.setWidth(100);
            infoTable.setBorderWidth(0.5f);
            infoTable.setPadding(4);

            addCell(infoTable, "Waybill No.", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10));
            addCell(infoTable, waybillNo, FontFactory.getFont(FontFactory.HELVETICA, 10));
            addCell(infoTable, "Date", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10));
            addCell(infoTable, LocalDateTime.now().format(DT_FMT), FontFactory.getFont(FontFactory.HELVETICA, 10));

            addCell(infoTable, "Ship From", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10));
            addCell(infoTable, str(params, "shipFrom", "N/A"), FontFactory.getFont(FontFactory.HELVETICA, 10));
            addCell(infoTable, "Ship To", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10));
            addCell(infoTable, str(params, "shipTo", "N/A"), FontFactory.getFont(FontFactory.HELVETICA, 10));

            addCell(infoTable, "Carrier", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10));
            addCell(infoTable, str(params, "carrier", "N/A"), FontFactory.getFont(FontFactory.HELVETICA, 10));
            addCell(infoTable, "Weight (kg)", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10));
            addCell(infoTable, str(params, "weight", "N/A"), FontFactory.getFont(FontFactory.HELVETICA, 10));

            doc.add(infoTable);
            doc.add(new Paragraph(" "));

            // ---------- 物料明细 ----------
            @SuppressWarnings("unchecked")
            List<Map<String, String>> items =
                    (List<Map<String, String>>) params.getOrDefault("items", Collections.emptyList());

            if (!items.isEmpty()) {
                Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
                Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 9);

                Table itemTable = new Table(5);
                itemTable.setWidth(100);
                itemTable.setBorderWidth(0.5f);
                itemTable.setPadding(3);

                addCell(itemTable, "No.", headerFont);
                addCell(itemTable, "Item Code", headerFont);
                addCell(itemTable, "Description", headerFont);
                addCell(itemTable, "Qty", headerFont);
                addCell(itemTable, "Unit", headerFont);

                int seq = 1;
                for (Map<String, String> item : items) {
                    addCell(itemTable, String.valueOf(seq++), bodyFont);
                    addCell(itemTable, item.getOrDefault("itemCode", ""), bodyFont);
                    addCell(itemTable, item.getOrDefault("description", ""), bodyFont);
                    addCell(itemTable, item.getOrDefault("qty", ""), bodyFont);
                    addCell(itemTable, item.getOrDefault("unit", ""), bodyFont);
                }
                doc.add(itemTable);
            }

            doc.add(new Paragraph(" "));

            // ---------- 备注 ----------
            String notes = str(params, "notes", "");
            if (!notes.isEmpty()) {
                Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
                doc.add(new Paragraph("Notes / 备注", labelFont));
                doc.add(new Paragraph(notes, FontFactory.getFont(FontFactory.HELVETICA, 10)));
            }

            // ---------- 签收栏 ----------
            doc.add(new Paragraph(" "));
            Table signTable = new Table(2);
            signTable.setWidth(100);
            signTable.setBorderWidth(0);
            addCell(signTable, "Shipper Signature: _______________", FontFactory.getFont(FontFactory.HELVETICA, 10));
            addCell(signTable, "Receiver Signature: _______________", FontFactory.getFont(FontFactory.HELVETICA, 10));
            doc.add(signTable);

            doc.close();
        } catch (DocumentException e) {
            throw new BusinessException(ErrorCode.REPORT_GENERATE_FAILED, "运单 PDF 渲染失败: " + e.getMessage());
        }
        return out.toByteArray();
    }

    private void addCell(Table table, String text, Font font) {
        table.addCell(new Phrase(text, font));
    }

    private String str(Map<String, Object> params, String key, String defaultValue) {
        Object val = params.get(key);
        return val != null ? String.valueOf(val) : defaultValue;
    }
}
