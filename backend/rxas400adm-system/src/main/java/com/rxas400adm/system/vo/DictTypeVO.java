package com.rxas400adm.system.vo;

import com.rxas400adm.system.entity.DictType;
import lombok.Data;

@Data
public class DictTypeVO {

    private Long id;
    private String code;
    private String name;
    private String remark;
    private Integer sort;
    private Integer status;

    public static DictTypeVO from(DictType entity) {
        DictTypeVO vo = new DictTypeVO();
        vo.setId(entity.getId());
        vo.setCode(entity.getCode());
        vo.setName(entity.getName());
        vo.setRemark(entity.getRemark());
        vo.setSort(entity.getSort());
        vo.setStatus(entity.getStatus());
        return vo;
    }
}