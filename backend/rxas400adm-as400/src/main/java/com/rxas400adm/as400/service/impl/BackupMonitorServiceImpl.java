package com.rxas400adm.as400.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rxas400adm.as400.entity.BackupStatus;
import com.rxas400adm.as400.entity.IbmiSystem;
import com.rxas400adm.as400.mapper.BackupStatusMapper;
import com.rxas400adm.as400.mapper.IbmiSystemMapper;
import com.rxas400adm.as400.service.IBackupMonitorService;
import com.rxas400adm.as400.vo.BackupStatusVO;
import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A4 备份监控服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BackupMonitorServiceImpl implements IBackupMonitorService {

    private final BackupStatusMapper backupStatusMapper;
    private final IbmiSystemMapper systemMapper;

    @Override
    public List<BackupStatusVO> listByServer(Long serverId) {
        LambdaQueryWrapper<BackupStatus> wrapper = new LambdaQueryWrapper<>();
        if (serverId != null) {
            wrapper.eq(BackupStatus::getServerId, serverId);
        }
        wrapper.orderByDesc(BackupStatus::getStartTime);
        List<BackupStatus> records = backupStatusMapper.selectList(wrapper);

        Map<Long, String> serverNames = systemMapper.selectList(null).stream()
                .collect(Collectors.toMap(IbmiSystem::getId, IbmiSystem::getName, (a, b) -> b));

        return records.stream().map(r -> {
            BackupStatusVO vo = new BackupStatusVO();
            vo.setId(r.getId());
            vo.setServerId(r.getServerId());
            vo.setServerName(serverNames.get(r.getServerId()));
            vo.setBackupName(r.getBackupName());
            vo.setBackupType(r.getBackupType());
            vo.setStatus(r.getStatus());
            vo.setStartTime(r.getStartTime());
            vo.setEndTime(r.getEndTime());
            vo.setDurationSeconds(r.getDurationSeconds());
            vo.setObjectsCount(r.getObjectsCount());
            vo.setSizeBytes(r.getSizeBytes());
            vo.setMediaName(r.getMediaName());
            vo.setErrorMessage(r.getErrorMessage());
            vo.setCreatedTime(r.getCreatedTime());
            return vo;
        }).toList();
    }

    @Override
    public BackupStatusVO getLatestByServer(Long serverId) {
        LambdaQueryWrapper<BackupStatus> wrapper = new LambdaQueryWrapper<>();
        if (serverId != null) {
            wrapper.eq(BackupStatus::getServerId, serverId);
        }
        wrapper.orderByDesc(BackupStatus::getStartTime).last(PageConstants.limitClause(1));
        BackupStatus record = backupStatusMapper.selectOne(wrapper);
        if (record == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "No backup status found for server: " + serverId);
        }

        IbmiSystem server = systemMapper.selectById(record.getServerId());
        BackupStatusVO vo = new BackupStatusVO();
        vo.setId(record.getId());
        vo.setServerId(record.getServerId());
        vo.setServerName(server != null ? server.getName() : null);
        vo.setBackupName(record.getBackupName());
        vo.setBackupType(record.getBackupType());
        vo.setStatus(record.getStatus());
        vo.setStartTime(record.getStartTime());
        vo.setEndTime(record.getEndTime());
        vo.setDurationSeconds(record.getDurationSeconds());
        vo.setObjectsCount(record.getObjectsCount());
        vo.setSizeBytes(record.getSizeBytes());
        vo.setMediaName(record.getMediaName());
        vo.setErrorMessage(record.getErrorMessage());
        vo.setCreatedTime(record.getCreatedTime());
        return vo;
    }
}
