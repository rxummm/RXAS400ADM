package com.rxas400adm.as400.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rxas400adm.as400.dto.DocTemplateDTO;
import com.rxas400adm.as400.entity.DocTemplate;
import com.rxas400adm.as400.mapper.DocTemplateMapper;
import com.rxas400adm.as400.vo.DocTemplateVO;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * 文档模板管理（从 system 模块迁入）：模板 CRUD。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocTemplateService {

    private final DocTemplateMapper templateMapper;

    public List<DocTemplateVO> listTemplates(String category) {
        LambdaQueryWrapper<DocTemplate> wrapper = new LambdaQueryWrapper<DocTemplate>()
                .orderByAsc(DocTemplate::getCategory).orderByAsc(DocTemplate::getName);
        if (StringUtils.hasText(category)) {
            wrapper.eq(DocTemplate::getCategory, category.trim());
        }
        return templateMapper.selectList(wrapper).stream().map(DocTemplateVO::from).toList();
    }

    public DocTemplate createTemplate(DocTemplateDTO dto, String operator) {
        DocTemplate template = new DocTemplate();
        template.setName(dto.getName());
        template.setCategory(dto.getCategory());
        template.setContent(dto.getContent());
        template.setDocType(normalizeType(dto.getDocType()));
        template.setId(null);
        template.setCreatedBy(operator);
        template.setCreatedTime(LocalDateTime.now());
        template.setUpdatedTime(LocalDateTime.now());
        templateMapper.insert(template);
        return template;
    }

    public void updateTemplate(Long id, DocTemplateDTO dto, String operator) {
        DocTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Document template not found");
        }
        if (StringUtils.hasText(dto.getName())) {
            template.setName(dto.getName().trim());
        }
        if (StringUtils.hasText(dto.getCategory())) {
            template.setCategory(dto.getCategory().trim());
        }
        if (dto.getContent() != null) {
            template.setContent(dto.getContent());
        }
        if (StringUtils.hasText(dto.getDocType())) {
            template.setDocType(normalizeType(dto.getDocType()));
        }
        template.setUpdatedTime(LocalDateTime.now());
        templateMapper.updateById(template);
    }

    public void deleteTemplate(Long id) {
        if (templateMapper.selectById(id) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Doc template not found: " + id);
        }
        templateMapper.deleteById(id);
    }

    /** 归一化文档类型：非法值按 MARKDOWN 兜底 */
    public String normalizeType(String docType) {
        if (!StringUtils.hasText(docType)) {
            return DocService.DOC_TYPE_MARKDOWN;
        }
        String upper = docType.trim().toUpperCase(Locale.ROOT);
        return switch (upper) {
            case DocService.DOC_TYPE_TEXT, DocService.DOC_TYPE_HTML,
                 DocService.DOC_TYPE_PDF, DocService.DOC_TYPE_IMAGE -> upper;
            default -> DocService.DOC_TYPE_MARKDOWN;
        };
    }
}
