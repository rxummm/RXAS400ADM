package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.dto.BpcsWabpConfigDTO;
import com.rxas400adm.as400.dto.BpcsWabpImportResult;
import com.rxas400adm.as400.sql.SqlStatementRegistry;
import com.rxas400adm.as400.util.BpcsRowUtil;
import com.rxas400adm.as400.vo.BpcsWabpConfigVO;
import com.rxas400adm.common.config.ProfileResolver;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.rxas400adm.as400.util.BpcsRowUtil.pickStr;

/**
 * ㊽ WABP 自动分货配置实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BpcsWabpServiceImpl implements IBpcsWabpService {

    private final AS400ClientProvider clientProvider;
    private final SqlStatementRegistry statements;
    private final ProfileResolver profileResolver;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HHmmss");

    @Override
    public List<BpcsWabpConfigVO> listConfigs(String cono, int limit) {
        if (cono == null || cono.isBlank()) cono = "001";
        if (profileResolver.isMockMode()) {
            return mockConfigs();
        }
        String sql = statements.get("bpcs.wabp.list");
        List<Map<String, Object>> rows = clientProvider.current().queryListCheckedBounded(sql, limit, cono, limit);
        List<BpcsWabpConfigVO> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            result.add(mapToVO(row));
        }
        return result;
    }

    @Override
    public BpcsWabpConfigVO getConfig(String cono, String wh, int dayOfWeek) {
        if (cono == null || cono.isBlank()) cono = "001";
        if (profileResolver.isMockMode()) {
            return mockConfigs().stream()
                    .filter(v -> v.wh().equals(wh) && v.dayOfWeek() == dayOfWeek)
                    .findFirst()
                    .orElse(null);
        }
        String sql = statements.get("bpcs.wabp.get");
        Map<String, Object> row = clientProvider.current().querySingleChecked(sql, cono, wh, dayOfWeek);
        return row != null ? mapToVO(row) : null;
    }

    @Override
    public void createConfig(String cono, BpcsWabpConfigDTO dto) {
        if (cono == null || cono.isBlank()) cono = "001";
        if (profileResolver.isMockMode()) {
            log.info("[MOCK] 创建 WABP 配置: cono={}, wh={}, dayOfWeek={}", cono, dto.getWh(), dto.getDayOfWeek());
            return;
        }
        String sql = statements.get("bpcs.wabp.insert");
        String maintDate = LocalDateTime.now().format(DATE_FMT);
        String maintTime = LocalDateTime.now().format(TIME_FMT);
        String maintUser = dto.getMaintUser() != null ? dto.getMaintUser() : "SYSTEM";
        clientProvider.current().executeUpdate(sql,
                cono,
                dto.getWh(),
                dto.getDayOfWeek(),
                dto.getTime(),
                dto.getShipHold(),
                dto.getCrHold(),
                dto.getPrHold(),
                dto.getActive(),
                maintUser,
                maintDate,
                maintTime
        );
    }

    @Override
    public void updateConfig(String cono, String wh, int dayOfWeek, BpcsWabpConfigDTO dto) {
        if (cono == null || cono.isBlank()) cono = "001";
        if (profileResolver.isMockMode()) {
            log.info("[MOCK] 更新 WABP 配置: cono={}, wh={}, dayOfWeek={}", cono, wh, dayOfWeek);
            return;
        }
        String sql = statements.get("bpcs.wabp.update");
        String maintDate = LocalDateTime.now().format(DATE_FMT);
        String maintTime = LocalDateTime.now().format(TIME_FMT);
        String maintUser = dto.getMaintUser() != null ? dto.getMaintUser() : "SYSTEM";
        int updated = clientProvider.current().executeUpdate(sql,
                dto.getTime(),
                dto.getShipHold(),
                dto.getCrHold(),
                dto.getPrHold(),
                dto.getActive(),
                maintUser,
                maintDate,
                maintTime,
                cono,
                wh,
                dayOfWeek
        );
        if (updated == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "WABP config not found: cono=" + cono + ", wh=" + wh + ", dayOfWeek=" + dayOfWeek);
        }
    }

    @Override
    public void deleteConfig(String cono, String wh, int dayOfWeek) {
        if (cono == null || cono.isBlank()) cono = "001";
        if (profileResolver.isMockMode()) {
            log.info("[MOCK] 删除 WABP 配置: cono={}, wh={}, dayOfWeek={}", cono, wh, dayOfWeek);
            return;
        }
        String sql = statements.get("bpcs.wabp.delete");
        int deleted = clientProvider.current().executeUpdate(sql, cono, wh, dayOfWeek);
        if (deleted == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "WABP config not found: cono=" + cono + ", wh=" + wh + ", dayOfWeek=" + dayOfWeek);
        }
    }

    @Override
    public List<BpcsWabpConfigDTO> parseExcel(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "File cannot be empty");
        }
        List<BpcsWabpConfigDTO> list = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                BpcsWabpConfigDTO dto = new BpcsWabpConfigDTO();
                dto.setWh(getCellString(row, 0));
                dto.setDayOfWeek(getCellInt(row, 1));
                dto.setTime(getCellString(row, 2));
                dto.setShipHold(getCellString(row, 3));
                dto.setCrHold(getCellString(row, 4));
                dto.setPrHold(getCellString(row, 5));
                dto.setActive(getCellString(row, 6));
                dto.setMaintUser("IMPORT");
                list.add(dto);
            }
        }
        return list;
    }

    @Override
    public BpcsWabpImportResult importConfigs(String cono, List<BpcsWabpConfigDTO> list) {
        if (cono == null || cono.isBlank()) cono = "001";
        BpcsWabpImportResult result = new BpcsWabpImportResult();
        result.setErrors(new ArrayList<>());
        for (int i = 0; i < list.size(); i++) {
            BpcsWabpConfigDTO dto = list.get(i);
            int rowNum = i + 1;
            try {
                // 校验必填
                if (dto.getWh() == null || dto.getWh().isBlank()) {
                    result.getErrors().add("Row " + rowNum + ": warehouse code is required");
                    result.setFailureCount(result.getFailureCount() + 1);
                    continue;
                }
                if (dto.getDayOfWeek() == null) {
                    result.getErrors().add("Row " + rowNum + ": day of week is required");
                    result.setFailureCount(result.getFailureCount() + 1);
                    continue;
                }
                // 尝试更新，不存在则插入
                BpcsWabpConfigVO existing = getConfig(cono, dto.getWh(), dto.getDayOfWeek());
                if (existing != null) {
                    updateConfig(cono, dto.getWh(), dto.getDayOfWeek(), dto);
                } else {
                    createConfig(cono, dto);
                }
                result.setSuccessCount(result.getSuccessCount() + 1);
            } catch (Exception e) {
                result.getErrors().add("第 " + rowNum + " 行: " + e.getMessage());
                result.setFailureCount(result.getFailureCount() + 1);
            }
        }
        return result;
    }

    @Override
    public List<BpcsWabpConfigVO> exportConfigs(String cono) {
        return listConfigs(cono, 1000);
    }

    private BpcsWabpConfigVO mapToVO(Map<String, Object> row) {
        return new BpcsWabpConfigVO(
                pickStr(row, "WH"),
                BpcsRowUtil.intOrNull(row, "DAY_OF_WEEK"),
                pickStr(row, "TIME"),
                pickStr(row, "SHPHOLD"),
                pickStr(row, "CRHOLD"),
                pickStr(row, "PRHOLD"),
                pickStr(row, "ACTIVE"),
                pickStr(row, "MAINT_USER"),
                BpcsRowUtil.dateStr(row, "MAINT_DATE")
        );
    }

    private String getCellString(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private Integer getCellInt(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return (int) cell.getNumericCellValue();
        }
        String val = getCellString(row, cellIndex);
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private List<BpcsWabpConfigVO> mockConfigs() {
        return List.of(
                new BpcsWabpConfigVO("WH1", 1, "14:00:00", "Y", "N", "N", "Y", "ADMIN", "20260829"),
                new BpcsWabpConfigVO("WH1", 3, "10:00:00", "Y", "Y", "N", "Y", "ADMIN", "20260829"),
                new BpcsWabpConfigVO("WH2", 1, "16:00:00", "Y", "N", "N", "N", "ADMIN", "20260828")
        );
    }
}