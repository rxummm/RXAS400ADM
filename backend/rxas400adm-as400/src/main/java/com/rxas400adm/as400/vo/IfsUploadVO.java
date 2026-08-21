package com.rxas400adm.as400.vo;

/**
 * IFS 文件上传结果（IfsController.upload 返回）。
 */
public record IfsUploadVO(String path, long size) {
}
