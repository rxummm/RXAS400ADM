package com.rxas400adm.as400.service;

import com.rxas400adm.as400.dto.UserProfileCreateDTO;
import com.rxas400adm.as400.dto.UserProfileUpdateDTO;
import com.rxas400adm.as400.model.UserProfileListRow;
import com.rxas400adm.as400.vo.UserProfileCreateResult;
import com.rxas400adm.as400.vo.UserProfileDetailVO;

import java.util.List;

/**
 * AS400用户Profile管理Service
 */
public interface IUserProfileService {

    /**
     * 获取用户Profile列表
     */
    List<UserProfileListRow> listUserProfiles();

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
    void deleteUserProfile(String userName, String operator);
}
