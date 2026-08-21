package com.rxas400adm.as400;

import com.rxas400adm.as400.model.IfsEntry;

import java.io.InputStream;
import java.util.List;

/**
 * IFS 域：目录浏览、文本/二进制读写、目录创建、回收站（历史目录）转移与恢复。
 */
public interface IfsClient {

    /** IFS 已删除文件历史目录（回收站）：逻辑删除 = 移入该目录，恢复 = 移回原路径 */
    String TRASH_ROOT = "/QOpenSys/rxas400/temp/as400histdocs";

    /**
     * 列出 IFS 目录（2.3.5）。目录不存在或不可达返回空列表。
     */
    List<IfsEntry> listIfsDir(String path);

    /**
     * 读取 IFS 文本文件内容（2.3.5）：失败返回空字符串（优雅降级）。
     */
    String readIfsFile(String path);

    /**
     * 写入 IFS 文本文件（文档管理「上传到 IFS」）：
     * 父目录不存在时自动创建；返回 true 表示写入成功。
     */
    boolean writeIfsFile(String path, String content);

    /**
     * 写入 IFS 二进制文件（任意附件上传）：
     * 父目录不存在时自动创建；返回 true 表示写入成功。
     */
    boolean writeIfsFileBytes(String path, byte[] content);

    /** 创建 IFS 目录（含父目录链，mkdirs 语义）；返回 true 表示创建成功 */
    boolean mkdirIfs(String path);

    /**
     * 逻辑删除 IFS 文件/目录：移入历史目录（回收站）而非真实删除。
     * 返回回收站中的新路径（null 表示失败）；rx_doc 记录删除走 DB 逻辑删除。
     */
    String trashIfsFile(String path);

    /**
     * 从回收站恢复 IFS 文件/目录：移回原路径（返回 true 表示成功）。
     * 仅对已移入历史目录的路径有效。
     */
    boolean restoreIfsFile(String trashPath);

    /**
     * 读取 IFS 文件原始字节（下载）：失败返回 null（优雅降级）。
     * 文本文件可由调用方按 UTF-8 转字符串。
     */
    byte[] readIfsFileBytes(String path);

    /**
     * 打开 IFS 文件输入流（流式下载，P0-3）：避免大文件整读内存。
     * 失败返回 null（优雅降级）；调用方负责关闭返回的流。
     */
    InputStream readIfsFileStream(String path);
}
