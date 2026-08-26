package com.rxas400adm.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.IfsClient;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.system.entity.Doc;
import com.rxas400adm.system.mapper.DocMapper;
import com.rxas400adm.system.vo.DocFileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.List;

/**
 * 文档 IFS 存储（从 DocService 拆分）：发布落盘、回收站、恢复、文件读取。
 *
 * <p>V49 文件型文档（PDF/IMAGE）正文为空，内容即 IFS 文件；
 * 类型化文档（MARKDOWN/TEXT/HTML）发布通过时自动落盘到受管目录。
 * <p>
 * 【T3】新增 IFS 发布补偿 sweep：approve 置 PUBLISHED 后 publishToIfs 失败的文档
 * （仅 log.warn 无重试入口）由定时任务重发布，幂等无害（见 resweepUnpublishedIfsDocs）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocStorageService {

    @Value("${rxas400.ifs.doc-root:/QOpenSys/rxas400/document}")
    private String docRoot = "/QOpenSys/rxas400/document";

    private final AS400ClientProvider clientProvider;
    private final DocMapper docMapper;

    /**
     * 发布通过时自动落盘 IFS：
     * - 无手动发布路径：写入受管目录 /document/{id}/doc.{ext} 并回填 ifsPath
     * - 已有受管路径：内容变更后重新发布时覆盖刷新
     * - 用户手动指定路径：不覆盖
     */
    public void publishToIfs(Doc doc) {
        String docType = normalizeType(doc.getDocType());
        if (isBinaryType(docType) || !StringUtils.hasText(doc.getContent())) {
            return;
        }
        String managedDir = docRoot + "/" + doc.getId() + "/";
        if (StringUtils.hasText(doc.getIfsPath())) {
            if (doc.getIfsPath().startsWith(managedDir)) {
                try {
                    IfsClient client = clientProvider.current();
                    if (client != null) {
                        client.writeIfsFile(doc.getIfsPath(), doc.getContent());
                    }
                } catch (Exception e) {
                    log.warn("文档重发刷新 IFS 失败(id={}, path={}): {}", doc.getId(), doc.getIfsPath(), e.getMessage());
                }
            }
            return;
        }
        String path = managedDir + "doc." + extForType(docType);
        try {
            IfsClient client = clientProvider.current();
            if (client != null && client.writeIfsFile(path, doc.getContent())) {
                // 终检 3b：条件 UPDATE 仅回写 ifs_path——sweep 持有的可能是陈旧整实体，
                // updateById 会把并发期间编辑过的标题/内容/status 用旧值整体覆盖（丢失更新）
                docMapper.update(null, new LambdaUpdateWrapper<Doc>()
                        .eq(Doc::getId, doc.getId())
                        .and(w -> w.isNull(Doc::getIfsPath).or().eq(Doc::getIfsPath, ""))
                        .set(Doc::getIfsPath, path));
                doc.setIfsPath(path);
            }
        } catch (Exception e) {
            log.warn("文档发布落盘 IFS 失败(id={}, path={}): {}", doc.getId(), path, e.getMessage());
        }
    }

    /**
     * 【T3】IFS 发布补偿 sweep：重发布「已 PUBLISHED 但 ifsPath 仍为空」的文档。
     * <p>
     * 幂等性说明：受管路径由 id 确定性推导（docRoot/{id}/doc.{ext}，见 publishToIfs），
     * 重复发布覆盖同一路径无害；用户手动指定路径的文档不在本 sweep 范围
     * （ifsPath 非空即被查询条件排除）。逐条尝试、成功 info、失败 warn 继续。
     * 调度可用性：启动类已 @EnableScheduling（Rxas400admApplication）。
     */
    @Scheduled(fixedDelayString = "${rxas400.doc.ifs-resweep-ms:1800000}")
    public void resweepUnpublishedIfsDocs() {
        List<Doc> pending;
        try {
            // 只读查询：PUBLISHED + 未逻辑删除 + ifsPath 为空 + 仅文本型（终检 3a：排除
            // PDF/IMAGE 二进制与空内容文本——publishToIfs 对其早退，纳入只会周期空转刷 warn）
            pending = docMapper.selectList(new LambdaQueryWrapper<Doc>()
                    .eq(Doc::getStatus, DocService.STATUS_PUBLISHED)
                    .eq(Doc::getDeleted, 0)
                    .notIn(Doc::getDocType, "PDF", "IMAGE")
                    .isNotNull(Doc::getContent)
                    .ne(Doc::getContent, "")
                    .and(w -> w.isNull(Doc::getIfsPath).or().eq(Doc::getIfsPath, "")));
        } catch (Exception e) {
            log.warn("【T3】IFS 补偿 sweep 查询失败: {}", e.getMessage());
            return;
        }
        if (pending.isEmpty()) {
            return;
        }
        log.info("【T3】IFS 补偿 sweep 开始，待重发布文档 {} 篇", pending.size());
        for (Doc doc : pending) {
            try {
                publishToIfs(doc);
                if (StringUtils.hasText(doc.getIfsPath())) {
                    log.info("【T3】文档补偿发布成功(id={}, path={})", doc.getId(), doc.getIfsPath());
                } else {
                    log.warn("【T3】文档补偿发布未落盘(id={}, type={})，下轮继续", doc.getId(), doc.getDocType());
                }
            } catch (Exception e) {
                log.warn("【T3】文档补偿发布失败(id={}): {}", doc.getId(), e.getMessage());
            }
        }
    }

    /** 移入 IFS 回收站（失败不阻塞 DB 逻辑删除） */
    public void trashIfsQuietly(String path) {
        try {
            IfsClient client = clientProvider.current();
            if (client != null) {
                client.trashIfsFile(path);
            }
        } catch (Exception e) {
            log.warn("IFS 移入回收站失败(path={}): {}", path, e.getMessage());
        }
    }

    /** 从 IFS 回收站恢复（失败不阻塞 DB 恢复） */
    public void restoreIfsQuietly(String originalPath) {
        try {
            IfsClient client = clientProvider.current();
            if (client != null) {
                client.restoreIfsFile(IfsClient.TRASH_ROOT + originalPath);
            }
        } catch (Exception e) {
            log.warn("IFS 恢复失败(path={}): {}", originalPath, e.getMessage());
        }
    }

    /** 读取 IFS 发布文件（V49 文件型预览） */
    public DocFileVO readFile(Long id, String ifsPath) {
        if (!StringUtils.hasText(ifsPath)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "该文档尚未发布到 IFS");
        }
        byte[] bytes = clientProvider.current().readIfsFileBytes(ifsPath);
        if (bytes == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "IFS 文件不存在或不可读: " + ifsPath);
        }
        String path = ifsPath.replace('\\', '/');
        String filename = path.substring(path.lastIndexOf('/') + 1);
        return new DocFileVO(bytes, contentTypeFor(filename), filename);
    }

    /** 文件型文档（正文为空，内容即 IFS 文件） */
    public boolean isBinaryType(String docType) {
        String t = normalizeType(docType);
        return DocService.DOC_TYPE_PDF.equals(t) || DocService.DOC_TYPE_IMAGE.equals(t);
    }

    /** 归一化文档类型：非法值按 MARKDOWN 兜底（共享逻辑，不依赖 DB） */
    private String normalizeType(String docType) {
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

    /** 按文档类型推导受管目录扩展名 */
    String extForType(String docType) {
        String normalized = normalizeType(docType);
        return switch (normalized) {
            case DocService.DOC_TYPE_TEXT -> "txt";
            case DocService.DOC_TYPE_HTML -> "html";
            default -> "md";
        };
    }

    /** 按扩展名推导 Content-Type（预览用，未知类型回退 octet-stream） */
    public String contentTypeFor(String filename) {
        int dot = filename.lastIndexOf('.');
        String ext = dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
        return switch (ext) {
            case "md", "markdown" -> "text/markdown; charset=utf-8";
            case "txt", "log", "ini", "conf", "csv" -> "text/plain; charset=utf-8";
            case "html", "htm" -> "text/html; charset=utf-8";
            case "json" -> "application/json; charset=utf-8";
            case "pdf" -> "application/pdf";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "bmp" -> "image/bmp";
            case "svg" -> "image/svg+xml";
            default -> "application/octet-stream";
        };
    }
}
