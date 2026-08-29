package com.rxas400adm.as400.vo;

/**
 * 文档 IFS 文件预览结果：原始字节 + 按扩展名推导的 Content-Type + 文件名。
 * 供 PDF / 图片等文件型文档在 img/iframe 中直接展示。
 */
public record DocFileVO(
        byte[] bytes,
        String contentType,
        String filename) {
}
