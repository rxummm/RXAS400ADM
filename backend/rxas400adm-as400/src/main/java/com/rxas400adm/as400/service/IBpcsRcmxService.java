package com.rxas400adm.as400.service;

import com.rxas400adm.as400.dto.BpcsRcmxConfigDTO;
import com.rxas400adm.as400.dto.BpcsRcmxImportResult;
import com.rxas400adm.as400.vo.*;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * ㊾ RCMX 客户 CSR 分配管理接口。
 */
public interface IBpcsRcmxService {

    /** 分页查询分配列表 */
    List<BpcsRcmxAssignmentVO> listAssignments(String cono, String custLike, String csrLike, int limit);

    /** 查询单条分配 */
    BpcsRcmxAssignmentVO getAssignment(String cono, String cust);

    /** 新增分配 */
    void createAssignment(String cono, BpcsRcmxConfigDTO dto);

    /** 更新分配 */
    void updateAssignment(String cono, String cust, BpcsRcmxConfigDTO dto);

    /** 删除分配 */
    void deleteAssignment(String cono, String cust);

    /** 客户选项（搜索 RCM） */
    List<BpcsCustOptionVO> searchCustomers(String cono, String keyword);

    /** CSR 选项（搜索 ECSR） */
    List<BpcsCsrOptionVO> searchCsrOptions(String cono, String keyword);

    /** 解析 Excel 文件为 DTO 列表 */
    List<BpcsRcmxConfigDTO> parseExcel(MultipartFile file) throws IOException;

    /** Excel 导入 */
    BpcsRcmxImportResult importAssignments(String cono, List<BpcsRcmxConfigDTO> list);

    /** Excel 导出数据 */
    List<BpcsRcmxAssignmentVO> exportAssignments(String cono);
}