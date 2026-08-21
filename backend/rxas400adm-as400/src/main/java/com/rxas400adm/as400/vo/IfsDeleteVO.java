package com.rxas400adm.as400.vo;

/**
 * IFS 删除（移入回收站）结果（IfsController.delete 返回）。
 */
public record IfsDeleteVO(String path, String trashPath) {
}
