package com.rxas400adm.as400.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用户Profile删除统计VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDeleteStatsVO {
    /** 总删除数 */
    private long totalDeletes;
    
    /** 手动删除数 */
    private long manualDeletes;
    
    /** 90天未登录删除数 */
    private long inactiveDeletes;
    
    /** 离职用户删除数 */
    private long resignedDeletes;
    
    /** 最近7天删除记录 */
    private List<DeleteRecordVO> recentRecords;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeleteRecordVO {
        private String userName;
        private String operator;
        private String deleteReason;
        private String deletionType;
        private String createdTime;
    }
}
