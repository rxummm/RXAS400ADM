package com.rxas400adm.operation.service;

import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.operation.domain.DesiredState;
import com.rxas400adm.operation.mapper.DesiredStateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DesiredStateService {

    private final DesiredStateMapper desiredStateMapper;

    public DesiredState get(String targetType, String targetName) {
        return desiredStateMapper.selectByTarget(targetType, targetName);
    }

    public List<DesiredState> listByType(String targetType) {
        return desiredStateMapper.selectListByTargetType(targetType);
    }

    public void upsert(String targetType, String targetName, String stateData, String createdBy) {
        desiredStateMapper.upsert(targetType, targetName, stateData, createdBy);
        log.info("Desired state upserted: type={}, name={}", targetType, targetName);
    }

    public boolean casUpdate(String targetType, String targetName, String stateData, int currentVersion) {
        int rows = desiredStateMapper.casUpdate(targetType, targetName, stateData, currentVersion);
        if (rows == 0) {
            log.warn("CAS update failed: type={}, name={}, version={}", targetType, targetName, currentVersion);
            return false;
        }
        return true;
    }
}