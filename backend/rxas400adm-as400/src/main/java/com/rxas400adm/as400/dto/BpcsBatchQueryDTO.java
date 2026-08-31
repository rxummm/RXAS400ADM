package com.rxas400adm.as400.dto;

import com.rxas400adm.common.constants.PageConstants;
import lombok.Data;

/**
 * 批次查询 DTO。
 */
@Data
public class BpcsBatchQueryDTO {
    private String cono = "001";
    private String lotno = "";
    private int current = 1;
    private int size = 100;

    public int getCurrent() { return (int) PageConstants.clampNum(current); }
    public int getSize() { return (int) PageConstants.clampSize(size); }
}
