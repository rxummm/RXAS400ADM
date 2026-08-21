package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.CommandResult;
import com.rxas400adm.as400.model.SubsystemRow;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 系统服务管理（2.3.7）：子系统状态列表（QSYS2.SUBSYSTEM_INFO）与启停操作，
 * 参照旧项目 IServiceController。数据源按 X-AS400-Server 路由。M2：返回 SubsystemRow。
 */
@Service
@RequiredArgsConstructor
public class SubsystemService implements ISubsystemService {

    private final AS400ClientProvider clientProvider;

    public List<SubsystemRow> list() {
        return clientProvider.current().listSubsystems();
    }

    public CommandResult start(String name) {
        require(name);
        return clientProvider.current().startSubsystem(name);
    }

    public CommandResult end(String name) {
        require(name);
        return clientProvider.current().endSubsystem(name);
    }

    private void require(String name) {
        if (!StringUtils.hasText(name)) {
            throw new BusinessException(ErrorCode.NAME_REQUIRED, "子系统名不能为空");
        }
    }
}
