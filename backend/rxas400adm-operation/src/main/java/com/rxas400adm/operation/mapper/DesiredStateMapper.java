package com.rxas400adm.operation.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rxas400adm.operation.domain.DesiredState;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DesiredStateMapper extends BaseMapper<DesiredState> {

    int upsert(@Param("targetType") String targetType, @Param("targetName") String targetName,
               @Param("stateData") String stateData, @Param("createdBy") String createdBy);

    int casUpdate(@Param("targetType") String targetType, @Param("targetName") String targetName,
                  @Param("stateData") String stateData, @Param("currentVersion") Integer currentVersion);

    /** 按 targetType 和 targetName 查询（避免 Service 层直接 new LambdaQueryWrapper） */
    default DesiredState selectByTarget(String targetType, String targetName) {
        return selectOne(new LambdaQueryWrapper<DesiredState>()
            .eq(DesiredState::getTargetType, targetType)
            .eq(DesiredState::getTargetName, targetName));
    }

    /** 按 targetType 查询并按 updatedAt 降序排列 */
    default List<DesiredState> selectListByTargetType(String targetType) {
        return selectList(new LambdaQueryWrapper<DesiredState>()
            .eq(DesiredState::getTargetType, targetType)
            .orderByDesc(DesiredState::getUpdatedAt));
    }
}