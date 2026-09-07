package com.rxas400adm.as400.service;

import com.rxas400adm.as400.vo.BackupStatusVO;

import java.util.List;

/**
 * A4 备份监控服务接口
 */
public interface IBackupMonitorService {

    /** 查询备份状态列表 */
    List<BackupStatusVO> listByServer(Long serverId);

    /** 获取备份统计摘要 */
    BackupStatusVO getLatestByServer(Long serverId);
}
