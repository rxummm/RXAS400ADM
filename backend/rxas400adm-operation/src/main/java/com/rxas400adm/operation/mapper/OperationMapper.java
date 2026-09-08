package com.rxas400adm.operation.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rxas400adm.operation.domain.Operation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OperationMapper extends BaseMapper<Operation> {

    int casUpdateStatus(@Param("id") Long id, @Param("targetStatus") String targetStatus,
                        @Param("currentVersion") Integer currentVersion);

    int updateStatus(@Param("id") Long id, @Param("targetStatus") String targetStatus,
                     @Param("errorCode") String errorCode, @Param("errorMessage") String errorMessage,
                     @Param("currentVersion") Integer currentVersion);

    int incrementRetry(@Param("id") Long id);

    int updateStartedAt(@Param("id") Long id, @Param("startedAt") LocalDateTime startedAt);

    int updateCurrentStep(@Param("id") Long id, @Param("currentStep") String currentStep);

    int updateCompletedAt(@Param("id") Long id, @Param("completedAt") LocalDateTime completedAt);

    Operation selectByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    int markFailed(@Param("id") Long id, @Param("errorCode") String errorCode,
                   @Param("errorMessage") String errorMessage, @Param("currentVersion") Integer currentVersion);

    List<Operation> findStuckOperations(@Param("threshold") LocalDateTime threshold, @Param("limit") int limit);

    /** 按 requestedAt 降序分页查询（避免 Service 层直接 new LambdaQueryWrapper） */
    default IPage<Operation> selectPageOrderByRequestedAt(Page<Operation> page) {
        return selectPage(page, new LambdaQueryWrapper<Operation>()
            .orderByDesc(Operation::getRequestedAt));
    }
}