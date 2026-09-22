package com.rxas400adm.as400.dto;

import lombok.Data;

import java.util.List;

/**
 * 批量删除用户Profile请求DTO
 */
@Data
public class UserProfileBatchDeleteDTO {
    /** 用户名列表 */
    private List<String> userNames;
    
    /** 删除原因 */
    private String deleteReason;
    
    /** 删除类型：MANUAL/INACTIVE_90D/AUTO_EXPIRE */
    private String deletionType;
}
