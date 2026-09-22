package com.rxas400adm.approval.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rxas400adm.approval.entity.ApprovalNotification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ApprovalNotificationMapper extends BaseMapper<ApprovalNotification> {

    @Select("<script>" +
            "SELECT * FROM rx_approval_notification" +
            " WHERE status = #{status} AND approver_name = #{approverName}" +
            " ORDER BY created_time DESC" +
            "</script>")
    List<ApprovalNotification> selectPendingByApprover(Page<ApprovalNotification> page,
                                                       @Param("status") String status,
                                                       @Param("approverName") String approverName);

    @Select("<script>" +
            "SELECT * FROM rx_approval_notification" +
            " WHERE 1=1" +
            " <if test='status != null'> AND status = #{status}</if>" +
            " <if test='targetType != null'> AND target_type = #{targetType}</if>" +
            " ORDER BY created_time DESC" +
            "</script>")
    List<ApprovalNotification> selectByFilters(Page<ApprovalNotification> page,
                                               @Param("status") String status,
                                               @Param("targetType") String targetType);

    @Select("SELECT COUNT(*) FROM rx_approval_notification" +
            " WHERE status = #{status} AND approver_name = #{approverName}")
    long countByStatusAndApprover(@Param("status") String status, @Param("approverName") String approverName);

    @Select("SELECT COUNT(*) FROM rx_approval_notification" +
            " WHERE status = #{status} AND approver_name = #{approverName}")
    long countByStatus(@Param("status") String status, @Param("approverName") String approverName);

    @Select("SELECT" +
            " SUM(CASE WHEN status = 'APPROVED' THEN 1 ELSE 0 END) AS approved," +
            " SUM(CASE WHEN status = 'REJECTED' THEN 1 ELSE 0 END) AS rejected" +
            " FROM rx_approval_notification" +
            " WHERE approver_name = #{approverName} AND status IN ('APPROVED', 'REJECTED')")
    long[] countApprovalStatsByApprover(@Param("approverName") String approverName);
}
