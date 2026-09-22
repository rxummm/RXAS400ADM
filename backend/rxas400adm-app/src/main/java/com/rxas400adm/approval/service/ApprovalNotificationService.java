package com.rxas400adm.approval.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rxas400adm.approval.dto.ApprovalActionDTO;
import com.rxas400adm.approval.entity.ApprovalNotification;
import com.rxas400adm.approval.mapper.ApprovalNotificationMapper;
import com.rxas400adm.approval.vo.ApprovalNotificationVO;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.common.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 审批通知服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalNotificationService {

    private final ApprovalNotificationMapper notificationMapper;

    /**
     * 查询当前用户的待审批列表
     *
     * <p>适用场景：移动端待办列表、用户个人待审批任务
     *
     * @param current 当前页码
     * @param size    每页数量
     * @return 分页结果
     */
    public PageResult<ApprovalNotificationVO> listPending(int current, int size) {
        String currentUsername = SecurityUtils.currentUsername();
        List<ApprovalNotification> records = notificationMapper.selectPendingByApprover(
                new Page<>(current, size), "PENDING", currentUsername);
        long total = notificationMapper.countByStatusAndApprover("PENDING", currentUsername);
        List<ApprovalNotificationVO> vos = records.stream()
                .map(ApprovalNotificationVO::from)
                .collect(Collectors.toList());
        return new PageResult<>(total, vos);
    }

    /**
     * 查询本用户的审批历史（支持可选筛选）
     *
     * <p>适用场景：用户查看个人审批记录、高级筛选查询（已按当前用户过滤）
     *
     * @param current    当前页码
     * @param size       每页数量
     * @param status     状态筛选（可选）
     * @param targetType 目标类型筛选（可选）
     * @return 分页结果
     */
    public PageResult<ApprovalNotificationVO> listAll(int current, int size, String status, String targetType) {
        List<ApprovalNotification> records = notificationMapper.selectByFilters(
                new Page<>(current, size), status, targetType);
        long total = notificationMapper.countByStatus(status, SecurityUtils.currentUsername());
        List<ApprovalNotificationVO> vos = records.stream()
                .map(ApprovalNotificationVO::from)
                .collect(Collectors.toList());
        return new PageResult<>(total, vos);
    }

    /**
     * 执行审批（带并发防护）
     *
     * <p>使用 CAS 风格条件更新：仅当状态仍为 PENDING 时才允许更新。
     * 若状态已被其他请求处理，则抛出业务异常。
     *
     * @param notificationId 审批通知ID
     * @param dto            审批动作DTO（包含 action 和 comment）
     * @return 更新后的审批通知VO
     */
    public ApprovalNotificationVO approve(Long notificationId, ApprovalActionDTO dto) {
        ApprovalNotification notification = notificationMapper.selectById(notificationId);
        if (notification == null) {
            throw new BusinessException(ErrorCode.APPROVAL_NOT_FOUND);
        }
        if (!"PENDING".equals(notification.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_STATUS_INVALID);
        }

        // CAS 风格更新：仅当状态仍为 PENDING 时更新
        int rows = notificationMapper.update(null, new LambdaUpdateWrapper<ApprovalNotification>()
                .set(ApprovalNotification::getStatus, dto.getAction())
                .set(ApprovalNotification::getAction, dto.getAction())
                .set(ApprovalNotification::getComment, dto.getComment())
                .set(ApprovalNotification::getUpdatedTime, LocalDateTime.now())
                .eq(ApprovalNotification::getId, notificationId)
                .eq(ApprovalNotification::getStatus, "PENDING"));

        if (rows == 0) {
            throw new BusinessException(ErrorCode.OPERATION_STATUS_INVALID, "审批状态已被其他人处理");
        }

        log.info("Approval completed: notificationId={}, action={}", notificationId, dto.getAction());
        notification.setStatus(dto.getAction());
        notification.setAction(dto.getAction());
        notification.setComment(dto.getComment());
        notification.setUpdatedTime(LocalDateTime.now());
        return ApprovalNotificationVO.from(notification);
    }

    /**
     * 获取当前用户的待审批数量
     *
     * @return 待审批数量
     */
    public long countPending() {
        String currentUsername = SecurityUtils.currentUsername();
        return notificationMapper.countByStatusAndApprover("PENDING", currentUsername);
    }

    /**
     * 获取审批统计卡片数据（已批准、已驳回数量）。
     * 注意：仅返回当前用户的统计结果，避免跨用户数据泄露。
     *
     * @return 审批统计数据，格式为 {approved, rejected}
     */
    public long[] countApprovalStats() {
        String currentUsername = SecurityUtils.currentUsername();
        return notificationMapper.countApprovalStatsByApprover(currentUsername);
    }
}
