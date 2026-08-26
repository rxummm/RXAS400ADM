package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.model.IfsEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;
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

    public List<IfsEntry> list(String path) {
        return clientProvider.current().listIfsDir(path);
    }

    public String read(String path) {
        return clientProvider.current().readIfsFile(path);
    }

    /** 写入 IFS 文本文件（文档管理「上传到 IFS」），父目录不存在自动创建 */
    public boolean write(String path, String content) {
        return clientProvider.current().writeIfsFile(path, content);
    }

    /** 写入 IFS 二进制文件（任意附件上传），父目录不存在自动创建 */
    public boolean writeBytes(String path, byte[] content) {
        return clientProvider.current().writeIfsFileBytes(path, content);
    }

    /** 打开 IFS 文件输入流（流式下载，P0-3）：失败返回 null，调用方负责关闭 */
    public InputStream readStream(String path) {
        return clientProvider.current().readIfsFileStream(path);
    }

    /** 新建目录（含父目录链） */
    public boolean mkdir(String path) {
        return clientProvider.current().mkdirIfs(path);
    }

    /** 逻辑删除 IFS 文件/目录：移入回收站（.trash），返回回收站路径（null 失败） */
    public String trash(String path) {
        return clientProvider.current().trashIfsFile(path);
    }

    /** 从回收站恢复 IFS 文件/目录（移回原路径） */
    public boolean restore(String trashPath) {
        return clientProvider.current().restoreIfsFile(trashPath);
    }
}
