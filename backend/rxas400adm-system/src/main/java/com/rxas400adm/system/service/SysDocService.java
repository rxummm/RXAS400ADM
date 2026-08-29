package com.rxas400adm.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.system.dto.SysDocDTO;
import com.rxas400adm.system.entity.SysDoc;
import com.rxas400adm.system.mapper.SysDocMapper;
import com.rxas400adm.system.vo.SysDocVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 知识库文档服务实现（V66）：纯 DB CRUD，无 IFS 存储。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysDocService implements ISysDocService {

    private static final String STATUS_DRAFT = "DRAFT";

    private final SysDocMapper sysDocMapper;

    @Override
    public PageResult<SysDocVO> list(String keyword, String status, String category,
                                      long current, long size) {
        LambdaQueryWrapper<SysDoc> wrapper = new LambdaQueryWrapper<SysDoc>()
                .orderByDesc(SysDoc::getUpdatedTime);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysDoc::getTitle, keyword.trim())
                    .or().like(SysDoc::getContent, keyword.trim()));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(SysDoc::getStatus, status.trim().toUpperCase());
        }
        if (StringUtils.hasText(category)) {
            wrapper.eq(SysDoc::getCategory, category.trim());
        }
        Page<SysDoc> page = sysDocMapper.selectPage(
                new Page<>(PageConstants.clampNum(current), PageConstants.clampSize(size)), wrapper);
        return new PageResult<>(page.getTotal(),
                page.getRecords().stream().map(SysDocVO::from).toList());
    }

    @Override
    public SysDocVO detail(Long id) {
        SysDoc doc = sysDocMapper.selectById(id);
        if (doc == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文档不存在");
        }
        return SysDocVO.from(doc);
    }

    @Override
    public SysDocVO create(SysDocDTO dto, String operator) {
        SysDoc doc = new SysDoc();
        doc.setTitle(dto.getTitle().trim());
        doc.setContent(dto.getContent());
        doc.setCategory(StringUtils.hasText(dto.getCategory()) ? dto.getCategory().trim() : null);
        doc.setTags(StringUtils.hasText(dto.getTags()) ? dto.getTags().trim() : null);
        doc.setStatus(STATUS_DRAFT);
        doc.setCreatedBy(operator);
        doc.setUpdatedBy(operator);
        doc.setCreatedTime(LocalDateTime.now());
        doc.setUpdatedTime(LocalDateTime.now());
        sysDocMapper.insert(doc);
        return SysDocVO.from(doc);
    }

    @Override
    public SysDocVO update(Long id, SysDocDTO dto, String operator) {
        SysDoc doc = sysDocMapper.selectById(id);
        if (doc == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文档不存在");
        }
        doc.setTitle(dto.getTitle().trim());
        if (dto.getContent() != null) {
            doc.setContent(dto.getContent());
        }
        if (dto.getCategory() != null) {
            doc.setCategory(dto.getCategory().trim());
        }
        if (dto.getTags() != null) {
            doc.setTags(dto.getTags().trim());
        }
        if (StringUtils.hasText(dto.getStatus())) {
            doc.setStatus(dto.getStatus().trim().toUpperCase());
        }
        doc.setUpdatedBy(operator);
        doc.setUpdatedTime(LocalDateTime.now());
        sysDocMapper.updateById(doc);
        return SysDocVO.from(doc);
    }

    @Override
    public void delete(Long id) {
        SysDoc doc = sysDocMapper.selectById(id);
        if (doc == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文档不存在");
        }
        sysDocMapper.deleteById(id);
    }
}
