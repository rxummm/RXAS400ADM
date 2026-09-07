package com.rxas400adm.email.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.common.util.EntityUtil;
import com.rxas400adm.email.dto.EmailGroupCreateDTO;
import com.rxas400adm.email.dto.EmailRecipientDTO;
import com.rxas400adm.email.entity.EmailRecipient;
import com.rxas400adm.email.entity.EmailRecipientGroup;
import com.rxas400adm.email.mapper.EmailRecipientGroupMapper;
import com.rxas400adm.email.mapper.EmailRecipientMapper;
import com.rxas400adm.email.vo.EmailGroupVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailGroupService implements IEmailGroupService {

    private final EmailRecipientGroupMapper groupMapper;
    private final EmailRecipientMapper recipientMapper;

    @Override
    public PageResult<EmailGroupVO> page(int current, int size, String keyword) {
        LambdaQueryWrapper<EmailRecipientGroup> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(EmailRecipientGroup::getGroupName, keyword.trim());
        }
        wrapper.orderByDesc(EmailRecipientGroup::getCreatedTime);
        Page<EmailRecipientGroup> page = groupMapper.selectPage(
                new Page<>(PageConstants.clampNum(current), PageConstants.clampSize(size)), wrapper);

        List<EmailGroupVO> records = page.getRecords().stream()
                .map(this::toGroupVO).toList();
        return new PageResult<>(page.getTotal(), records);
    }

    @Override
    public List<EmailGroupVO> listAll() {
        return groupMapper.selectList(
                new LambdaQueryWrapper<EmailRecipientGroup>().orderByAsc(EmailRecipientGroup::getGroupName)
        ).stream().map(this::toGroupVO).toList();
    }

    private EmailGroupVO toGroupVO(EmailRecipientGroup g) {
        long count = recipientMapper.selectCount(
                new LambdaQueryWrapper<EmailRecipient>()
                        .eq(EmailRecipient::getGroupId, g.getId())
                        .eq(EmailRecipient::getEnabled, 1));
        return EmailGroupVO.from(g, (int) count);
    }

    @Override
    public void create(EmailGroupCreateDTO dto) {
        EmailRecipientGroup group = new EmailRecipientGroup();
        group.setGroupName(dto.getGroupName());
        group.setDescription(dto.getDescription());
        group.setCreatedTime(LocalDateTime.now());
        group.setUpdatedTime(LocalDateTime.now());
        groupMapper.insert(group);
    }

    @Override
    public void update(Long id, EmailGroupCreateDTO dto) {
        EmailRecipientGroup group = EntityUtil.require(id, "Email Group", groupMapper::selectById);
        group.setGroupName(dto.getGroupName());
        group.setDescription(dto.getDescription());
        group.setUpdatedTime(LocalDateTime.now());
        groupMapper.updateById(group);
    }

    @Override
    public void delete(Long id) {
        EntityUtil.require(id, "Email Group", groupMapper::selectById);
        groupMapper.deleteById(id);
        recipientMapper.delete(new LambdaQueryWrapper<EmailRecipient>()
                .eq(EmailRecipient::getGroupId, id));
    }

    @Override
    public List<EmailRecipient> members(Long id) {
        return recipientMapper.selectList(
                new LambdaQueryWrapper<EmailRecipient>()
                        .eq(EmailRecipient::getGroupId, id)
                        .orderByAsc(EmailRecipient::getEmail));
    }

    @Override
    public void addMember(Long id, EmailRecipientDTO dto) {
        EntityUtil.require(id, "Email Group", groupMapper::selectById);
        Long exists = recipientMapper.selectCount(new LambdaQueryWrapper<EmailRecipient>()
                .eq(EmailRecipient::getGroupId, id)
                .eq(EmailRecipient::getEmail, dto.getEmail()));
        if (exists > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Email address already in group");
        }
        EmailRecipient recipient = new EmailRecipient();
        recipient.setGroupId(id);
        recipient.setEmail(dto.getEmail());
        recipient.setUserId(dto.getUserId());
        recipient.setEnabled(1);
        recipient.setCreatedTime(LocalDateTime.now());
        recipientMapper.insert(recipient);
    }

    @Override
    public void removeMember(Long id, Long memberId) {
        EntityUtil.require(memberId, "Email Recipient", recipientMapper::selectById);
        recipientMapper.deleteById(memberId);
    }
}