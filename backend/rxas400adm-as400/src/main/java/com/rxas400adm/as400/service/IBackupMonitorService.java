package com.rxas400adm.as400.service;

import com.rxas400adm.as400.vo.BackupStatusVO;
import com.rxas400adm.common.response.PageResult;

import java.util.List;

/**
 * A4 备份监控服务接口
 */
public interface IBackupMonitorService {

    /** 查询备份状态列表（分页） */
    PageResult<BackupStatusVO> listByServer(Long serverId, int current, int size);

    /** 查询备份状态列表（兼容旧版，不分页） */
    @Deprecated
    default List<BackupStatusVO> listByServer(Long serverId) {
        return listByServer(serverId, 1, Integer.MAX_VALUE).getRecords();
    }

    /** 获取备份统计摘要 */
    BackupStatusVO getLatestByServer(Long serverId);
}
