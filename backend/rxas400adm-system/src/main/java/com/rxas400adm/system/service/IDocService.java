package com.rxas400adm.system.service;

import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.system.dto.DocDTO;
import com.rxas400adm.system.dto.DocTemplateDTO;
import com.rxas400adm.system.entity.Doc;
import com.rxas400adm.system.entity.DocTemplate;
import com.rxas400adm.system.vo.DocFileVO;
import com.rxas400adm.system.vo.DocTemplateVO;
import com.rxas400adm.system.vo.DocVersionVO;

import java.util.List;

/**
 * 文档管理服务接口（模板起草 → 版本管理 → 审批流 → 发布）。
 */
public interface IDocService {

    String STATUS_DRAFT = "DRAFT";
    String STATUS_PENDING = "PENDING";
    String STATUS_PUBLISHED = "PUBLISHED";
    String STATUS_REJECTED = "REJECTED";

    List<DocTemplateVO> listTemplates(String category);

    DocTemplate createTemplate(DocTemplateDTO template, String operator);

    void updateTemplate(Long id, DocTemplateDTO template, String operator);

    void deleteTemplate(Long id);

    PageResult<Doc> listDocs(String keyword, String status, long current, long size, boolean deletedOnly);

    Doc createDoc(DocDTO doc, String operator);

    Doc updateDoc(Long id, DocDTO update, String operator);

    void submit(Long id, String operator);

    void approve(Long id, String operator);

    void reject(Long id, String reason, String operator);

    void delete(Long id);

    void restore(Long id);

    void purge(Long id);

    void rollback(Long id, Integer version, String operator);

    List<DocVersionVO> versions(Long id);

    DocFileVO file(Long id);

    Doc detail(Long id);
}