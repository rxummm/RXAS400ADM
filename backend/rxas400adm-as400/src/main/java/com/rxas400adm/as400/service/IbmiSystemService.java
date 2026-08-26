package com.rxas400adm.as400.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rxas400adm.as400.AS400Client;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.CommandResult;
import com.rxas400adm.as400.dto.IbmiSystemDTO;
import com.rxas400adm.common.crypto.AesCryptoService;
import com.rxas400adm.as400.entity.IbmiSystem;
import com.rxas400adm.as400.mapper.IbmiSystemMapper;
import com.rxas400adm.as400.vo.EnabledServerVO;
import com.rxas400adm.as400.vo.IbmiSystemVO;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.common.security.DangerousClCommandValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class IbmiSystemService implements IIbmiSystemService {

    private final IbmiSystemMapper systemMapper;
    private final AS400ClientProvider clientProvider;
    private final AesCryptoService aesCryptoService;
    /** S4：高危 CL 动词黑名单校验（管理员通道执行兜底） */
    private final DangerousClCommandValidator clValidator;

    public List<IbmiSystem> list() {
        return systemMapper.selectList(new LambdaQueryWrapper<IbmiSystem>()
                .orderByAsc(IbmiSystem::getSortOrder)
                .orderByDesc(IbmiSystem::getCreatedTime));
    }

    /** N2：普通用户可见的服务器列表（不含 username/passwordEncrypt 连接凭据） */
    public List<IbmiSystemVO> listVO() {
        return list().stream().map(IbmiSystemVO::from).toList();
    }

    /** 启用的服务器列表（登录页下拉公开接口用，仅返回启用项）
     *  P1-5 加固：公开接口只返回最小视图（id/name/environment），不暴露 host/username/port。 */
    public List<EnabledServerVO> listEnabled() {
        return systemMapper.selectList(new LambdaQueryWrapper<IbmiSystem>()
                        .eq(IbmiSystem::getEnabled, true)
                        .orderByAsc(IbmiSystem::getSortOrder)
                        .orderByDesc(IbmiSystem::getCreatedTime))
                .stream()
                .map(s -> new EnabledServerVO(s.getId(), s.getName(), s.getEnvironment()))
                .toList();
    }

    public IbmiSystem get(Long id) {
        IbmiSystem system = systemMapper.selectById(id);
        if (system == null) {
            throw new BusinessException(ErrorCode.AS400_SERVER_NOT_FOUND);
        }
        return system;
    }

    public IbmiSystem create(IbmiSystemDTO dto) {
        IbmiSystem system = toEntity(dto);
        system.setId(null);
        assertUniqueName(system.getName(), null);
        if (system.getEnabled() == null) {
            system.setEnabled(true);
        }
        if (system.getCcsid() == null) {
            system.setCcsid(37);
        }
        if (system.getPort() == null) {
            system.setPort(8470);
        }
        if (StringUtils.hasText(system.getPasswordEncrypt())) {
            system.setPasswordEncrypt(aesCryptoService.encrypt(system.getPasswordEncrypt()));
        }
        system.setCreatedTime(LocalDateTime.now());
        systemMapper.insert(system);
        clientProvider.evict(system.getId());
        return system;
    }

    public IbmiSystem update(Long id, IbmiSystemDTO dto) {
        IbmiSystem existing = get(id);
        IbmiSystem system = toEntity(dto);
        system.setId(id);
        assertUniqueName(system.getName(), id);
        if (StringUtils.hasText(system.getPasswordEncrypt())) {
            system.setPasswordEncrypt(aesCryptoService.encrypt(system.getPasswordEncrypt()));
        } else {
            system.setPasswordEncrypt(existing.getPasswordEncrypt());
        }
        systemMapper.updateById(system);
        // S2：改主机/账号/密码/启停后立即失效客户端连接与配置缓存，无需重启
        clientProvider.evict(id);
        return system;
    }

    public void delete(Long id) {
        systemMapper.deleteById(id);
        clientProvider.evict(id);
    }

    /** DTO → 实体映射（仅业务字段；id/status/connectionStatus/createdTime 由服务端托管） */
    private IbmiSystem toEntity(IbmiSystemDTO dto) {
        IbmiSystem system = new IbmiSystem();
        system.setName(dto.getName());
        system.setHost(dto.getHost());
        system.setPort(dto.getPort());
        system.setUsername(dto.getUsername());
        system.setPasswordEncrypt(dto.getPasswordEncrypt());
        system.setEnvironment(dto.getEnvironment());
        system.setRegion(dto.getRegion());
        system.setCriticalLevel(dto.getCriticalLevel());
        system.setHaGroup(dto.getHaGroup());
        system.setSslEnabled(dto.getSslEnabled());
        system.setDefaultLibraries(dto.getDefaultLibraries());
        system.setCcsid(dto.getCcsid());
        system.setEnabled(dto.getEnabled());
        system.setDefaultServer(dto.getDefaultServer());
        system.setDescription(dto.getDescription());
        system.setSortOrder(dto.getSortOrder());
        return system;
    }

    /** P3：rx_ibmi_system.name 唯一约束的入口校验（create/update 友好报错，避免撞唯一索引 500） */
    private void assertUniqueName(String name, Long excludeId) {
        if (!StringUtils.hasText(name)) {
            return; // DB 层 NOT NULL 兜底
        }
        LambdaQueryWrapper<IbmiSystem> wrapper = new LambdaQueryWrapper<IbmiSystem>()
                .eq(IbmiSystem::getName, name.trim());
        if (excludeId != null) {
            wrapper.ne(IbmiSystem::getId, excludeId);
        }
        if (systemMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "服务器名称已存在：" + name.trim());
        }
    }

    /** 连接测试：针对指定服务器创建客户端并验证登录 */
    public CommandResult testConnection(Long id) {
        IbmiSystem system = get(id);
        CommandResult result = clientProvider.forServer(id).testConnection();
        system.setStatus(result.success() ? "ONLINE" : "OFFLINE");
        systemMapper.updateById(system);
        // 连接测试可能重建了客户端（如首次连接成功），使配置缓存立即反映最新状态
        clientProvider.evict(id);
        return result;
    }

    /** 在指定服务器上执行 CL 命令 */
    public CommandResult executeCommand(Long id, String command) {
        if (!StringUtils.hasText(command)) {
            throw new BusinessException(ErrorCode.NAME_REQUIRED, "命令不能为空");
        }
        // S4：该入口需 AS400_MANAGE 权限（管理员通道），仍执行黑名单兜底，通过后留审计日志注明通道
        clValidator.assertAllowed(command);
        log.info("[IBMi系统] 管理员通道执行 CL 命令: serverId={}, command={}", id, command);
        AS400Client client = clientProvider.forServer(id);
        return client.execute(command);
    }
}