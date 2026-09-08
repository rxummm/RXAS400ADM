package com.rxas400adm.operation.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rxas400adm.operation.domain.OperationStep;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OperationStepMapper extends BaseMapper<OperationStep> {

    int updateStatus(@Param("operationId") Long operationId, @Param("stepCode") String stepCode,
                     @Param("status") String status, @Param("version") Integer version);

    int updateResult(@Param("operationId") Long operationId, @Param("stepCode") String stepCode,
                     @Param("status") String status, @Param("ibmiReturnCode") String ibmiReturnCode,
                     @Param("ibmiMessage") String ibmiMessage, @Param("errorDetail") String errorDetail,
                     @Param("durationMs") Long durationMs, @Param("completedAt") LocalDateTime completedAt,
                     @Param("version") Integer version);

    /** 按 operationId 查询步骤列表（避免 Service 层直接 new LambdaQueryWrapper） */
    default List<OperationStep> selectListByOperationId(Long operationId) {
        return selectList(new LambdaQueryWrapper<OperationStep>()
            .eq(OperationStep::getOperationId, operationId));
    }
}