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

    /**
     * IFS 路径归一化 + 安全校验（沙箱）：
     * ① 仅接受绝对路径（以 / 开头）；② 拒绝 .. 路径穿越与反斜杠混淆；
     * ③ 拒绝以 . 开头的隐藏段；④ 去除尾部多余斜杠；⑤ 根目录白名单校验。
     * 非法路径抛 BAD_REQUEST；空/blank 返回 null。
     */
    String normalizeAndValidate(String path);

    /**
     * 校验上传文件参数：非空 + 大小上限检查。
     * 非法参数抛 BAD_REQUEST。
     */
    void validateUploadFile(org.springframework.web.multipart.MultipartFile file, long maxUploadBytes);
}
