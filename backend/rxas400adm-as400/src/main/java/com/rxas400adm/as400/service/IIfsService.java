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

    boolean write(String path, String content);

    boolean writeBytes(String path, byte[] content);

    byte[] readBytes(String path);

    /** 打开 IFS 文件输入流（流式下载，避免大文件整读内存）；失败返回 null，调用方负责关闭 */
    InputStream readStream(String path);

    boolean mkdir(String path);

    String trash(String path);

    boolean restore(String trashPath);
}
