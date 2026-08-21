package com.rxas400adm.as400.vo;

import java.util.Map;

/**
 * 物理文件数据条目（PfController.data 返回）。
 */
public record PfDataVO(Map<String, Object> data) {
    public static PfDataVO from(Map<String, Object> map) {
        return new PfDataVO(map);
    }
}
