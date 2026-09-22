package com.rxas400adm.as400.service;

import com.rxas400adm.as400.dto.UserProfileBatchDeleteDTO;
import com.rxas400adm.as400.dto.UserProfileCreateDTO;
import com.rxas400adm.as400.dto.UserProfileUpdateDTO;
import com.rxas400adm.as400.entity.UserProfileLog;
import com.rxas400adm.as400.model.UserProfileListRow;
import com.rxas400adm.as400.vo.UserProfileBatchDeleteResultVO;
import com.rxas400adm.as400.vo.UserProfileCreateResult;
import com.rxas400adm.as400.vo.UserProfileDeleteStatsVO;
import com.rxas400adm.as400.vo.UserProfileDetailVO;
import com.rxas400adm.common.response.PageResult;

/**
 * AS400用户Profile管理Service
 */
public interface IUserProfileService {

    /**
     * 获取用户Profile列表（分页）
     */
    PageResult<UserProfileListRow> listUserProfiles(int current, int size, String keyword);

    /**
     * 获取用户Profile详情
     */
    UserProfileDetailVO getUserProfile(String userName);

    /**
     * 创建用户Profile
     */
    UserProfileCreateResult createUserProfile(UserProfileCreateDTO dto, String operator);

    /**
     * 更新用户Profile
     */
    void updateUserProfile(String userName, UserProfileUpdateDTO dto, String operator);

    /**
     * 删除用户Profile
     */
    void deleteUserProfile(String userName, String operator, String deleteReason, String deletionType);

    /**
     * 批量删除用户Profile
     */
    UserProfileBatchDeleteResultVO batchDeleteUserProfiles(UserProfileBatchDeleteDTO dto, String operator);

    /**
     * 获取用户Profile操作日志（分页）
     */
    PageResult<UserProfileLog> getProfileLogs(String userName, int current, int size);

    /**
     * 获取删除统计信息
     */
    UserProfileDeleteStatsVO getDeleteStats();
}
