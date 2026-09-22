package com.rxas400adm.edi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.edi.entity.EdiDocument;
import com.rxas400adm.edi.mapper.EdiDocumentMapper;
import com.rxas400adm.edi.vo.EdiDocumentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class EdiDocumentService {

    private final EdiDocumentMapper mapper;

    public PageResult<EdiDocumentVO> pageQuery(String cono, String documentType, String direction,
                                               String status, LocalDateTime fromDate, LocalDateTime toDate,
                                               int current, int size) {
        LambdaQueryWrapper<EdiDocument> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(cono)) wrapper.eq(EdiDocument::getCono, cono);
        if (StringUtils.hasText(documentType)) wrapper.eq(EdiDocument::getDocumentType, documentType);
        if (StringUtils.hasText(direction)) wrapper.eq(EdiDocument::getDirection, direction);
        if (StringUtils.hasText(status)) wrapper.eq(EdiDocument::getStatus, status);
        if (fromDate != null) wrapper.ge(EdiDocument::getCreatedTime, fromDate);
        if (toDate != null) wrapper.le(EdiDocument::getCreatedTime, toDate);
        wrapper.orderByDesc(EdiDocument::getCreatedTime);

        Page<EdiDocument> page = mapper.selectPage(new Page<>(current, size), wrapper);
        return new PageResult<>(page.getTotal(), page.getRecords().stream().map(EdiDocumentVO::from).toList());
    }

    public EdiDocumentVO getById(Long id) {
        EdiDocument entity = mapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return EdiDocumentVO.from(entity);
    }
}
