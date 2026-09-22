package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.model.IfsEntry;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * IFS 文件浏览（2.3.5）：目录列表 + 文本文件读取，数据源按 X-AS400-Server 路由。
 * V32 增强：上传/下载/新建目录/删除（IFS 页文件管理 + 文档管理「上传到 IFS」）。
 * M2：返回类型收敛为 record（IfsEntry），JSON 键保持不变。
 */
@Service
@RequiredArgsConstructor
public class IfsService implements IIfsService {

    private final AS400ClientProvider clientProvider;

    /**
     * 允许访问的 IFS 根目录白名单（逗号分隔）。默认限定平台工作目录 /QOpenSys/rxas400，
     * 防止任意路径读写连接账号可达的系统文件（/etc、/QOpenSys/usr/bin 等）。
     */
    @Value("${rxas400.ifs.allowed-roots:/QOpenSys/rxas400}")
    private String allowedRoots;

    public List<IfsEntry> list(String path) {
        return clientProvider.current().listIfsDir(path);
    }

    public String read(String path) {
        return clientProvider.current().readIfsFile(path);
    }

    /** 写入 IFS 文本文件（文档管理「上传到 IFS」），父目录不存在自动创建 */
    public void write(String path, String content) {
        if (!clientProvider.current().writeIfsFile(path, content)) {
            throw new BusinessException(ErrorCode.AS400_COMMAND_FAILED, "IFS write failed: " + path);
        }
    }

    /** 写入 IFS 二进制文件（任意附件上传），父目录不存在自动创建 */
    public void writeBytes(String path, byte[] content) {
        if (!clientProvider.current().writeIfsFileBytes(path, content)) {
            throw new BusinessException(ErrorCode.AS400_COMMAND_FAILED, "IFS upload failed: " + path);
        }
    }

    /** 打开 IFS 文件输入流（流式下载，P0-3）：文件不存在时抛 NOT_FOUND */
    public InputStream readStream(String path) {
        InputStream in = clientProvider.current().readIfsFileStream(path);
        if (in == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "IFS file not found or unreadable: " + path);
        }
        return in;
    }

    /** 新建目录（含父目录链） */
    public void mkdir(String path) {
        if (!clientProvider.current().mkdirIfs(path)) {
            throw new BusinessException(ErrorCode.AS400_COMMAND_FAILED, "IFS mkdir failed: " + path);
        }
    }

    /** 逻辑删除 IFS 文件/目录：移入回收站（.trash），返回回收站路径 */
    public String trash(String path) {
        String trashPath = clientProvider.current().trashIfsFile(path);
        if (trashPath == null) {
            throw new BusinessException(ErrorCode.AS400_COMMAND_FAILED, "IFS trash failed: " + path);
        }
        return trashPath;
    }

    /** 从回收站恢复 IFS 文件/目录（移回原路径） */
    public void restore(String trashPath) {
        if (!clientProvider.current().restoreIfsFile(trashPath)) {
            throw new BusinessException(ErrorCode.AS400_COMMAND_FAILED, "IFS restore failed: " + trashPath);
        }
    }

    /**
     * IFS 沙箱（P2-8）：统一归一化并校验路径——
     * ① 仅接受绝对路径（以 / 开头）；② 拒绝 .. 路径穿越与反斜杠混淆（先转 / 再校验）；
     * ③ 拒绝以 . 开头的隐藏段；④ 去除尾部多余斜杠；⑤ 根目录白名单校验。
     * 非法路径抛 BAD_REQUEST（不再直达 IFS 客户端，防止越权读写连接账号可达的任意路径）。
     */
    @Override
    public String normalizeAndValidate(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String normalized = path.replace('\\', '/');
        if (!normalized.startsWith("/")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "IFS path must be absolute (start with /): " + path);
        }
        for (String segment : normalized.split("/")) {
            if (segment.equals("..")) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "IFS path must not contain .. : " + path);
            }
            if (segment.startsWith(".") && !segment.isEmpty()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "IFS path must not access hidden segments: " + path);
            }
        }
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        final String candidate = normalized;
        final String rootsConfig = allowedRoots;
        boolean allowed = Arrays.stream(rootsConfig.split(","))
                .map(String::trim)
                .filter(root -> !root.isEmpty())
                .anyMatch(root -> candidate.equals(root) || candidate.startsWith(root + "/"));
        if (!allowed) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "IFS path is not within allowed roots (rxas400.ifs.allowed-roots): " + path);
        }
        return normalized;
    }

    @Override
    public void validateUploadFile(MultipartFile file, long maxUploadBytes) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Upload file is required");
        }
        if (file.getSize() > maxUploadBytes) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Upload file exceeds size limit (" + (maxUploadBytes / 1024 / 1024) + "MB)");
        }
    }
}
