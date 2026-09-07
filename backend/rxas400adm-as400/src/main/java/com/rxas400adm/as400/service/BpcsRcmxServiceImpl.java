package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.dto.BpcsRcmxConfigDTO;
import com.rxas400adm.as400.dto.BpcsRcmxImportResult;
import com.rxas400adm.as400.sql.SqlStatementRegistry;
import com.rxas400adm.as400.util.BpcsRowUtil;
import com.rxas400adm.as400.vo.BpcsCsrOptionVO;
import com.rxas400adm.as400.vo.BpcsCustOptionVO;
import com.rxas400adm.as400.vo.BpcsRcmxAssignmentVO;
import com.rxas400adm.common.constants.As400Identifiers;
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
 * ㊾ RCMX 客户 CSR 分配管理实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BpcsRcmxServiceImpl implements IBpcsRcmxService {

    private final AS400ClientProvider clientProvider;
    private final SqlStatementRegistry statements;
    private final ProfileResolver profileResolver;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HHmmss");

    @Override
    public List<BpcsRcmxAssignmentVO> listAssignments(String cono, String custLike, String csrLike, int limit) {
        if (cono == null || cono.isBlank()) cono = "001";
        validate(cono);
        if (profileResolver.isMockMode()) {
            return mockAssignments();
        }
        String sql = statements.get("bpcs.rcmx.list");
        List<Map<String, Object>> rows = clientProvider.current().queryListCheckedBounded(sql, limit, cono, limit);
        List<BpcsRcmxAssignmentVO> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            result.add(mapToVO(row));
        }
        return result;
    }

    @Override
    public BpcsRcmxAssignmentVO getAssignment(String cono, String cust) {
        if (cono == null || cono.isBlank()) cono = "001";
        validate(cono);
        if (profileResolver.isMockMode()) {
            return mockAssignments().stream()
                    .filter(v -> v.cust().equals(cust))
                    .findFirst()
                    .orElse(null);
        }
        String sql = statements.get("bpcs.rcmx.get");
        Map<String, Object> row = clientProvider.current().querySingleChecked(sql, cono, cust);
        return row != null ? mapToVO(row) : null;
    }

    @Override
    public void createAssignment(String cono, BpcsRcmxConfigDTO dto) {
        if (cono == null || cono.isBlank()) cono = "001";
        validate(cono);
        // 唯一性检查
        if (profileResolver.isMockMode()) {
            log.info("[MOCK] 创建 RCMX 分配: cono={}, cust={}, csrId={}", cono, dto.getCust(), dto.getCsrId());
            return;
        }
        // 检查是否已有活跃分配
        String checkSql = statements.get("bpcs.rcmx.checkUnique");
        Long count = clientProvider.current().queryForObject(checkSql, Long.class, cono, dto.getCust());
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Customer already assigned to CSR, deactivate existing first: " + dto.getCust());
        }
        String sql = statements.get("bpcs.rcmx.insert");
        String maintDate = LocalDateTime.now().format(DATE_FMT);
        String maintTime = LocalDateTime.now().format(TIME_FMT);
        String maintUser = dto.getMaintUser() != null ? dto.getMaintUser() : "SYSTEM";
        clientProvider.current().executeUpdate(sql,
                cono,
                dto.getCust(),
                dto.getCsrId(),
                dto.getActive(),
                maintUser,
                maintDate,
                maintTime
        );
    }

    @Override
    public void updateAssignment(String cono, String cust, BpcsRcmxConfigDTO dto) {
        if (cono == null || cono.isBlank()) cono = "001";
        validate(cono);
        if (profileResolver.isMockMode()) {
            log.info("[MOCK] 更新 RCMX 分配: cono={}, cust={}", cono, cust);
            return;
        }
        // 如果变更 CSR 且设置为活跃，检查唯一性
        if ("Y".equals(dto.getActive())) {
            String checkSql = statements.get("bpcs.rcmx.checkUnique");
            Long count = clientProvider.current().queryForObject(checkSql, Long.class, cono, cust);
            if (count != null && count > 0) {
                // 先停用现有的
                String deactivateSql = statements.get("bpcs.rcmx.update");
                clientProvider.current().executeUpdate(deactivateSql, "", "N", "SYSTEM", 
                        LocalDateTime.now().format(DATE_FMT), LocalDateTime.now().format(TIME_FMT), cono, cust);
            }
        }
        String sql = statements.get("bpcs.rcmx.update");
        String maintDate = LocalDateTime.now().format(DATE_FMT);
        String maintTime = LocalDateTime.now().format(TIME_FMT);
        String maintUser = dto.getMaintUser() != null ? dto.getMaintUser() : "SYSTEM";
        int updated = clientProvider.current().executeUpdate(sql,
                dto.getCsrId(),
                dto.getActive(),
                maintUser,
                maintDate,
                maintTime,
                cono,
                cust
        );
        if (updated == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "RCMX assignment not found: cono=" + cono + ", cust=" + cust);
        }
    }

    @Override
    public void deleteAssignment(String cono, String cust) {
        if (cono == null || cono.isBlank()) cono = "001";
        validate(cono);
        if (profileResolver.isMockMode()) {
            log.info("[MOCK] 删除 RCMX 分配: cono={}, cust={}", cono, cust);
            return;
        }
        String sql = statements.get("bpcs.rcmx.delete");
        int deleted = clientProvider.current().executeUpdate(sql, cono, cust);
        if (deleted == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "RCMX assignment not found: cono=" + cono + ", cust=" + cust);
        }
    }

    @Override
    public List<BpcsCustOptionVO> searchCustomers(String cono, String keyword) {
        if (cono == null || cono.isBlank()) cono = "001";
        if (profileResolver.isMockMode()) {
            return List.of(new BpcsCustOptionVO("20315", "张三科技"), new BpcsCustOptionVO("20316", "王五制造"));
        }
        String like = "%" + (keyword != null ? keyword : "") + "%";
        String sql = statements.get("bpcs.rcmx.custOptions");
        List<Map<String, Object>> rows = clientProvider.current().queryListCheckedBounded(sql, 20, cono, like, like);
        List<BpcsCustOptionVO> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            result.add(new BpcsCustOptionVO(pickStr(row, "CUST"), pickStr(row, "CUNAME")));
        }
        return result;
    }

    @Override
    public List<BpcsCsrOptionVO> searchCsrOptions(String cono, String keyword) {
        if (cono == null || cono.isBlank()) cono = "001";
        if (profileResolver.isMockMode()) {
            return List.of(new BpcsCsrOptionVO("CSR001", "李四"), new BpcsCsrOptionVO("CSR002", "赵六"));
        }
        String like = "%" + (keyword != null ? keyword : "") + "%";
        String sql = statements.get("bpcs.rcmx.csrOptions");
        List<Map<String, Object>> rows = clientProvider.current().queryListCheckedBounded(sql, 20, cono, like, like);
        List<BpcsCsrOptionVO> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            result.add(new BpcsCsrOptionVO(pickStr(row, "EMPID"), pickStr(row, "NAME")));
        }
        return result;
    }

    @Override
    public List<BpcsRcmxConfigDTO> parseExcel(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "File cannot be empty");
        }
        List<BpcsRcmxConfigDTO> list = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                BpcsRcmxConfigDTO dto = new BpcsRcmxConfigDTO();
                dto.setCust(getCellString(row, 0));
                dto.setCsrId(getCellString(row, 1));
                dto.setActive(getCellString(row, 2));
                dto.setMaintUser("IMPORT");
                list.add(dto);
            }
        }
        return list;
    }

    @Override
    public BpcsRcmxImportResult importAssignments(String cono, List<BpcsRcmxConfigDTO> list) {
        if (cono == null || cono.isBlank()) cono = "001";
        BpcsRcmxImportResult result = new BpcsRcmxImportResult();
        result.setErrors(new ArrayList<>());
        for (int i = 0; i < list.size(); i++) {
            BpcsRcmxConfigDTO dto = list.get(i);
            int rowNum = i + 1;
            try {
                if (dto.getCust() == null || dto.getCust().isBlank()) {
                    result.getErrors().add("Row " + rowNum + ": customer code is required");
                    result.setFailureCount(result.getFailureCount() + 1);
                    continue;
                }
                if (dto.getCsrId() == null || dto.getCsrId().isBlank()) {
                    result.getErrors().add("Row " + rowNum + ": CSR ID is required");
                    result.setFailureCount(result.getFailureCount() + 1);
                    continue;
                }
                BpcsRcmxAssignmentVO existing = getAssignment(cono, dto.getCust());
                if (existing != null) {
                    updateAssignment(cono, dto.getCust(), dto);
                } else {
                    createAssignment(cono, dto);
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
    public List<BpcsRcmxAssignmentVO> exportAssignments(String cono) {
        return listAssignments(cono, null, null, 1000);
    }

    private BpcsRcmxAssignmentVO mapToVO(Map<String, Object> row) {
        return new BpcsRcmxAssignmentVO(
                pickStr(row, "CUST"),
                pickStr(row, "CUNAME"),
                pickStr(row, "RESPCSR"),
                null,
                pickStr(row, "ACTIVE"),
                pickStr(row, "MAINT_USER"),
                BpcsRowUtil.dateStr(row, "MAINT_DATE")
        );
    }

    private List<BpcsRcmxAssignmentVO> mockAssignments() {
        return List.of(
                new BpcsRcmxAssignmentVO("20315", "张三科技", "CSR001", "李四", "Y", "ADMIN", "20260829"),
                new BpcsRcmxAssignmentVO("20316", "王五制造", "CSR002", "赵六", "N", "ADMIN", "20260828"),
                new BpcsRcmxAssignmentVO("20317", "孙七贸易", "CSR001", "李四", "Y", "ADMIN", "20260827")
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

    private void validate(String cono) {
        if (!As400Identifiers.IDENTIFIER.matcher(cono.toUpperCase()).matches()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid company code: " + cono);
        }
    }
}