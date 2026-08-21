package com.rxas400adm.system.service;

import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.system.entity.PermissionRequest;

import java.util.List;

/**
 * 权限自助申请服务接口（rx_permission_request）。
 */
public interface IPermissionRequestService {

    String REQUESTED_ROLE = "REQUESTED";

    PermissionRequest create(String username, String permissionCode, List<Long> menuIds,
                             List<String> menuNames, String reason);

    PageResult<PermissionRequest> mine(String username, int current, int size);

    PageResult<PermissionRequest> adminPage(int current, int size, String status, String keyword);

    long pendingCount();

    PermissionRequest approve(Long id, String approver, String comment);

    PermissionRequest reject(Long id, String approver, String comment);
}
