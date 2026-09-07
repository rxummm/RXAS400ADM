package com.rxas400adm.as400.service;

import com.rxas400adm.as400.model.IfsEntry;

import java.io.InputStream;
import java.util.List;

/**
 * IFS 文件浏览：目录列表、文本/二进制读写、新建目录与回收站管理。
 * 数据源按 X-AS400-Server 路由。
 */
public interface IIfsService {

    List<IfsEntry> list(String path);

    String read(String path);

    void write(String path, String content);

    void writeBytes(String path, byte[] content);

    /** 打开 IFS 文件输入流（流式下载，避免大文件整读内存）；文件不存在时抛 NOT_FOUND */
    InputStream readStream(String path);

    void mkdir(String path);

    String trash(String path);

    void restore(String trashPath);
}
