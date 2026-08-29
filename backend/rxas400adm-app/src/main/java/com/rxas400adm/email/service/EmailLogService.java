package com.rxas400adm.email.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.email.entity.EmailLog;
import com.rxas400adm.email.mapper.EmailLogMapper;
import com.rxas400adm.email.vo.EmailLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class EmailLogService implements IEmailLogService {

    private final EmailLogMapper emailLogMapper;

    @Override
    public PageResult<EmailLogVO> page(int current, int size, String channel, String status, String keyword) {
        LambdaQueryWrapper<EmailLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(channel)) {
            wrapper.eq(EmailLog::getChannel, channel);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(EmailLog::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(EmailLog::getSubject, keyword.trim())
                    .or().like(EmailLog::getRecipients, keyword.trim()));
        }
        wrapper.orderByDesc(EmailLog::getCreatedTime);
        Page<EmailLog> page = emailLogMapper.selectPage(
                new Page<>(PageConstants.clampNum(current), PageConstants.clampSize(size)), wrapper);
        return new PageResult<>(page.getTotal(),
                page.getRecords().stream().map(EmailLogVO::from).toList());
    }

    @Override
    public EmailLogVO detail(Long id) {
        EmailLog log = emailLogMapper.selectById(id);
        return log != null ? EmailLogVO.from(log) : null;
    }
}
