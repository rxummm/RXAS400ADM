package com.rxas400adm.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.system.entity.Doc;
import com.rxas400adm.system.entity.DocVersion;
import com.rxas400adm.system.mapper.DocMapper;
import com.rxas400adm.system.mapper.DocVersionMapper;
import com.rxas400adm.system.vo.DocVersionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文档版本管理（从 DocService 拆分）：版本快照、回滚、版本历史查询。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocVersionService {

    private final DocMapper docMapper;
    private final DocVersionMapper versionMapper;

    public List<DocVersionVO> versions(Long docId) {
        return versionMapper.selectList(new LambdaQueryWrapper<DocVersion>()
                .eq(DocVersion::getDocId, docId)
                .orderByDesc(DocVersion::getVersion))
                .stream().map(DocVersionVO::from).toList();
    }

    /**
     * P12 版本正文只读懒加载：版本列表已瘦身不含 content，按版本 id 单点取回正文大字段。
     */
    public String versionContent(Long versionId) {
        DocVersion ver = versionMapper.selectById(versionId);
        if (ver == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "版本快照不存在");
        }
        return ver.getContent();
    }

    /**
     * 从指定版本快照恢复为当前编辑态。
     * 已发布则提升版本号并回到草稿重新走审批。
     */
    public void rollback(Long id, Integer version, String operator) {
        Doc doc = docMapper.selectById(id);
        if (doc == null || (doc.getDeleted() != null && doc.getDeleted() == 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文档不存在");
        }
        DocVersion snapshot = versionMapper.selectOne(new LambdaQueryWrapper<DocVersion>()
                .eq(DocVersion::getDocId, id)
                .eq(DocVersion::getVersion, version));
        if (snapshot == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "版本快照不存在");
        }
        if (DocService.STATUS_PENDING.equals(doc.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "文档审批中不可回滚，请先驳回或等待结果");
        }
        doc.setTitle(snapshot.getTitle());
        doc.setContent(snapshot.getContent());
        if (DocService.STATUS_PUBLISHED.equals(doc.getStatus())) {
            doc.setVersion((doc.getVersion() == null ? 1 : doc.getVersion()) + 1);
        }
        doc.setStatus(DocService.STATUS_DRAFT);
        doc.setUpdatedBy(operator);
        doc.setUpdatedTime(LocalDateTime.now());
        docMapper.updateById(doc);
        snapshot(doc, operator);
    }

    /** 保存版本快照（幂等：同版本不重复留档） */
    public void snapshot(Doc doc, String operator) {
        Long exists = versionMapper.selectCount(new LambdaQueryWrapper<DocVersion>()
                .eq(DocVersion::getDocId, doc.getId())
                .eq(DocVersion::getVersion, doc.getVersion()));
        if (exists != null && exists > 0) {
            return;
        }
        DocVersion ver = new DocVersion();
        ver.setDocId(doc.getId());
        ver.setVersion(doc.getVersion());
        ver.setTitle(doc.getTitle());
        ver.setContent(doc.getContent());
        ver.setOperator(operator);
        ver.setCreatedTime(LocalDateTime.now());
        versionMapper.insert(ver);
    }

    /** 永久删除指定文档的所有版本历史 */
    public void deleteVersions(Long docId) {
        versionMapper.delete(new LambdaQueryWrapper<DocVersion>()
                .eq(DocVersion::getDocId, docId));
    }
}
