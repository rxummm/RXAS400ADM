package com.rxas400adm.as400.dto;

import com.rxas400adm.common.constants.PageConstants;
import lombok.Data;

/**
 * 仓库查询 DTO。
 */
@Data
public class BpcsWarehouseQueryDTO {
    private String cono = "001";
    private String whse = "";
    private String whname = "";
    private int current = 1;
    private int size = 100;

    public int getCurrent() { return (int) PageConstants.clampNum(current); }
    public int getSize() { return (int) PageConstants.clampSize(size); }
}
