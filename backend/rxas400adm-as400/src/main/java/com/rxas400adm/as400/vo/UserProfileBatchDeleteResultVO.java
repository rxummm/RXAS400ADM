package com.rxas400adm.as400.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 批量删除用户Profile响应VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileBatchDeleteResultVO {
    /** 总请求数 */
    private int total;
    
    /** 成功删除数 */
    private int successCount;
    
    /** 失败数 */
    private int failCount;
    
    /** 失败详情 */
    private String failDetails;
}
