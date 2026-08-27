package com.rxas400adm.as400.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.rxas400adm.as400.AS400Client;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.CommandResult;
import com.rxas400adm.as400.dto.CommandScriptRequest;
import com.rxas400adm.as400.entity.CommandScript;
import com.rxas400adm.common.constants.ExecutionStatus;
import com.rxas400adm.common.util.EntityUtil;
import com.rxas400adm.as400.mapper.CommandScriptMapper;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.common.security.DangerousClCommandValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 命令脚本中心（2.4.3）：保存/复用 CL 命令，收藏与标签分类，一键对指定服务器执行。
 */
@Service
@RequiredArgsConstructor
public class CommandScriptService implements ICommandScriptService {

    private final CommandScriptMapper scriptMapper;
    private final AS400ClientProvider clientProvider;
    /** S4：高危 CL 动词黑名单校验（执行期兜底） */
    private final DangerousClCommandValidator clValidator;

    /** P16b：tags 聚合缓存——tags() 为内存聚合且每次请求全量拉脚本表，用 60s TTL 缓存削峰，写操作失效 */
    private final Cache<String, List<String>> tagsCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(60))
            .maximumSize(1)
            .build();

    public List<CommandScript> list(Boolean favorite, String tag) {
        LambdaQueryWrapper<CommandScript> wrapper = new LambdaQueryWrapper<>();
        if (Boolean.TRUE.equals(favorite)) {
            wrapper.eq(CommandScript::getFavorite, true);
        }
        if (StringUtils.hasText(tag)) {
            wrapper.like(CommandScript::getTags, tag.trim());
        }
        wrapper.orderByDesc(CommandScript::getFavorite).orderByDesc(CommandScript::getUpdatedTime);
        return scriptMapper.selectList(wrapper);
    }

    /** 全部标签（去重；P16b：60s 缓存，写操作失效） */
    public List<String> tags() {
        return tagsCache.get("tags", key -> loadTagsFromDb());
    }

    /** P16b：tags 实际聚合逻辑（原实现，全量拉脚本表后内存拆分去重） */
    private List<String> loadTagsFromDb() {
        Set<String> result = new LinkedHashSet<>();
        for (CommandScript script : scriptMapper.selectList(null)) {
            if (StringUtils.hasText(script.getTags())) {
                for (String tag : script.getTags().split(",")) {
                    if (StringUtils.hasText(tag.trim())) {
                        result.add(tag.trim());
                    }
                }
            }
        }
        return List.copyOf(result);
    }

    
    public CommandScript create(CommandScriptRequest request, String username) {
        CommandScript script = new CommandScript();
        apply(script, request);
        script.setCreatedBy(username);
        script.setRunCount(0);
        script.setLastRunStatus(null);
        script.setCreatedTime(LocalDateTime.now());
        script.setUpdatedTime(LocalDateTime.now());
        scriptMapper.insert(script);
        tagsCache.invalidate("tags"); // P16b：写操作失效 tags 缓存
        return script;
    }

    
    public CommandScript update(Long id, CommandScriptRequest request) {
        CommandScript script = EntityUtil.require(id, "命令脚本", scriptMapper::selectById);
        apply(script, request);
        script.setUpdatedTime(LocalDateTime.now());
        scriptMapper.updateById(script);
        tagsCache.invalidate("tags"); // P16b：写操作失效 tags 缓存
        return script;
    }

    
    public void delete(Long id) {
        EntityUtil.require(id, "命令脚本", scriptMapper::selectById);
        scriptMapper.deleteById(id);
        tagsCache.invalidate("tags"); // P16b：写操作失效 tags 缓存
    }

    
    public CommandScript toggleFavorite(Long id, Boolean favorite) {
        CommandScript script = EntityUtil.require(id, "命令脚本", scriptMapper::selectById);
        script.setFavorite(Boolean.TRUE.equals(favorite));
        script.setUpdatedTime(LocalDateTime.now());
        scriptMapper.updateById(script);
        return script;
    }

    /** 对指定服务器执行脚本（记录执行次数/结果） */
    public CommandResult execute(Long id, Long serverId) {
        CommandScript script = EntityUtil.require(id, "命令脚本", scriptMapper::selectById);
        if (serverId == null) {
            throw new BusinessException(ErrorCode.AS400_SERVER_REQUIRED, "请选择执行服务器");
        }
        // S4：执行前高危动词兜底（创建侧无黑名单校验，此处防历史脏数据/直改库绕过）
        clValidator.assertAllowed(script.getCommand());
        AS400Client client = clientProvider.forServer(serverId);
        CommandResult result = client.execute(script.getCommand());
        script.setRunCount((script.getRunCount() == null ? 0 : script.getRunCount()) + 1);
        script.setLastRunTime(LocalDateTime.now());
        script.setLastResult(result.message());
        // M1：结构化状态落库，供 ExecutionService/ReportService 直接读，不解析中文字串
        script.setLastRunStatus(result.success() ? ExecutionStatus.SUCCESS : ExecutionStatus.FAILED);
        script.setUpdatedTime(LocalDateTime.now());
        scriptMapper.updateById(script);
        return result;
    }



    private void apply(CommandScript script, CommandScriptRequest request) {
        script.setName(request.getName());
        script.setDescription(request.getDescription());
        script.setCommand(request.getCommand());
        script.setFavorite(Boolean.TRUE.equals(request.getFavorite()));
        script.setTags(request.getTags());
    }
}