package com.rxas400adm.system.vo;

import com.rxas400adm.system.entity.DictItem;
import lombok.Data;

@Data
public class DictItemVO {

    private Long id;
    private String typeCode;
    private String itemKey;
    private String itemValue;
    private Integer sort;
    private Integer status;

    public static DictItemVO from(DictItem entity) {
        DictItemVO vo = new DictItemVO();
        vo.setId(entity.getId());
        vo.setTypeCode(entity.getTypeCode());
        vo.setItemKey(entity.getItemKey());
        vo.setItemValue(entity.getItemValue());
        vo.setSort(entity.getSort());
        vo.setStatus(entity.getStatus());
        return vo;
    }
}