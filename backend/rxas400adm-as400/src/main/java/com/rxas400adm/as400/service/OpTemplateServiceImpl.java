package com.rxas400adm.as400.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rxas400adm.as400.AS400Client;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.CommandResult;
import com.rxas400adm.as400.dto.OpTemplateCreateDTO;
import com.rxas400adm.as400.dto.OpTemplateUpdateDTO;
import com.rxas400adm.as400.entity.OpTemplate;
import com.rxas400adm.as400.mapper.OpTemplateMapper;
import com.rxas400adm.as400.vo.OpTemplateVO;
import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.common.util.EntityUtil;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.common.security.DangerousClCommandValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 操作模板 Service 实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpTemplateServiceImpl implements IOpTemplateService {

    private final OpTemplateMapper opTemplateMapper;
    private final AS400ClientProvider clientProvider;
    private final ObjectMapper objectMapper;
    /** S4：高危 CL 动词黑名单校验（每步执行前兜底） */
    private final DangerousClCommandValidator clValidator;

    @Override
    public PageResult<OpTemplateVO> page(long current, long size, String keyword) {
        LambdaQueryWrapper<OpTemplate> wrapper = new LambdaQueryWrapper<OpTemplate>()
                .orderByDesc(OpTemplate::getId);
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(OpTemplate::getName, kw).or().like(OpTemplate::getDescription, kw));
        }
        Page<OpTemplate> result = opTemplateMapper.selectPage(
                new Page<>(PageConstants.clampNum(current), PageConstants.clampSize(size)), wrapper);
        return new PageResult<>(result.getTotal(),
                result.getRecords().stream().map(OpTemplateVO::from).toList());
    }

    @Override
    public OpTemplateVO create(OpTemplateCreateDTO dto, String username) {
        parseSteps(dto.getSteps());
        OpTemplate template = new OpTemplate();
        template.setName(dto.getName());
        template.setDescription(dto.getDescription());
        template.setSteps(dto.getSteps());
        template.setCreatedBy(username);
        template.setCreatedAt(LocalDateTime.now());
        opTemplateMapper.insert(template);
        return OpTemplateVO.from(template);
    }

    @Override
    public OpTemplateVO update(Long id, OpTemplateUpdateDTO dto, String username) {
        parseSteps(dto.getSteps());
        OpTemplate template = EntityUtil.require(id, "模板", opTemplateMapper::selectById);
        template.setName(dto.getName());
        template.setDescription(dto.getDescription());
        template.setSteps(dto.getSteps());
        template.setUpdatedBy(username);
        template.setUpdatedAt(LocalDateTime.now());
        opTemplateMapper.updateById(template);
        return OpTemplateVO.from(template);
    }

    @Override
    public void delete(Long id) {
        EntityUtil.require(id, "模板", opTemplateMapper::selectById);
        opTemplateMapper.deleteById(id);
    }

    @Override
    public void execute(Long id, Long serverId) {
        OpTemplate template = EntityUtil.require(id, "模板", opTemplateMapper::selectById);
        AS400Client client = clientProvider.forServer(serverId);
        List<Map<String, String>> steps = parseSteps(template.getSteps());
        /* B9：历史脏数据兜底，空步骤直接拒绝执行 */
        if (steps.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "操作模板步骤不能为空");
        }

        for (Map<String, String> step : steps) {
            String command = step.get("command");
            if (command != null && !command.isBlank()) {
                // S4：每步执行前高危动词兜底（模板步骤可能由历史数据带入危险命令）
                clValidator.assertAllowed(command);
                CommandResult result = client.execute(command);
                log.info("模板[{}]执行命令: {} -> {}", template.getName(), command, result.success() ? "OK" : "FAIL");
                if (!result.success()) {
                    throw new BusinessException(ErrorCode.AS400_COMMAND_FAILED,
                            "命令执行失败: " + command + " - " + result.message());
                }
            }
        }
    }



    /** 解析并校验步骤 JSON；非法 JSON 直接拒绝（写入前拦截，避免脏数据落库后执行期才失败） */
    private List<Map<String, String>> parseSteps(String stepsJson) {
        try {
            /* B9：字面量 "null" 合法 JSON 但解析为 null，需显式拒绝 */
            List<Map<String, String>> steps = objectMapper.readValue(stepsJson, new TypeReference<>() {});
            if (steps == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "操作模板步骤不能为空");
            }
            return steps;
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "模板步骤不是合法的 JSON 数组");
        }
    }
}
