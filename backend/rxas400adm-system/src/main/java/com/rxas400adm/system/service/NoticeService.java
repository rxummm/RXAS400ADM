package com.rxas400adm.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.common.util.EntityUtil;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.system.dto.NoticeDTO;
import com.rxas400adm.system.entity.Notice;
import com.rxas400adm.system.mapper.NoticeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统公告（rx_notice）：管理端发布/下架，发布时向全体活跃用户写站内通知。
 */
@Service
@RequiredArgsConstructor
public class NoticeService implements INoticeService {

    private final NoticeMapper noticeMapper;
    private final INotificationService notificationService;

    public List<Notice> published() {
        return noticeMapper.selectList(new LambdaQueryWrapper<Notice>()
                .eq(Notice::getStatus, 1)
                .orderByDesc(Notice::getPublishedTime));
    }

    public PageResult<Notice> page(int current, int size, String keyword, Integer status) {
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Notice::getTitle, keyword.trim());
        }
        if (status != null) {
            wrapper.eq(Notice::getStatus, status);
        }
        wrapper.orderByDesc(Notice::getCreatedTime);
        Page<Notice> page = noticeMapper.selectPage(
                new Page<>(PageConstants.clampNum(current), PageConstants.clampSize(size)), wrapper);
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    
    public Notice create(NoticeDTO dto, String username) {
        Notice notice = new Notice();
        notice.setTitle(dto.getTitle());
        notice.setContent(dto.getContent());
        notice.setStatus(dto.getStatus());
        if (!StringUtils.hasText(notice.getTitle()) || !StringUtils.hasText(notice.getContent())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "标题与内容必填");
        }
        notice.setId(null);
        notice.setStatus(notice.getStatus() == null ? 1 : notice.getStatus());
        notice.setCreatedBy(username);
        notice.setCreatedTime(LocalDateTime.now());
        notice.setUpdatedTime(LocalDateTime.now());
        noticeMapper.insert(notice);
        // 发布即推送全体活跃用户站内通知
        if (notice.getStatus() == 1) {
            notice.setPublishedTime(LocalDateTime.now());
            noticeMapper.updateById(notice);
            notificationService.sendToAllActiveUsers("NOTICE", notice.getTitle(), notice.getContent());
        }
        return notice;
    }

    
    public Notice update(Long id, NoticeDTO dto) {
        Notice notice = EntityUtil.require(id, "公告", noticeMapper::selectById);
        if (StringUtils.hasText(dto.getTitle())) {
            notice.setTitle(dto.getTitle());
        }
        if (dto.getContent() != null) {
            notice.setContent(dto.getContent());
        }
        if (dto.getStatus() != null && !dto.getStatus().equals(notice.getStatus())) {
            notice.setStatus(dto.getStatus());
            if (dto.getStatus() == 1) {
                notice.setPublishedTime(LocalDateTime.now());
                notificationService.sendToAllActiveUsers("NOTICE", notice.getTitle(), notice.getContent());
            }
        }
        notice.setUpdatedTime(LocalDateTime.now());
        noticeMapper.updateById(notice);
        return notice;
    }

    
    public void delete(Long id) {
        noticeMapper.deleteById(EntityUtil.require(id, "公告", noticeMapper::selectById).getId());
    }
}