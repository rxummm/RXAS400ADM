package com.rxas400adm.system.vo;

import com.rxas400adm.system.entity.Region;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RegionVO {

    private Long id;
    private String code;
    private String name;
    private Integer level;
    private String parentCode;
    private String pinyin;
    private String abbreviation;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer sort;
    private Integer status;
    private LocalDateTime createdTime;

    public static RegionVO from(Region entity) {
        RegionVO vo = new RegionVO();
        vo.setId(entity.getId());
        vo.setCode(entity.getCode());
        vo.setName(entity.getName());
        vo.setLevel(entity.getLevel());
        vo.setParentCode(entity.getParentCode());
        vo.setPinyin(entity.getPinyin());
        vo.setAbbreviation(entity.getAbbreviation());
        vo.setLongitude(entity.getLongitude());
        vo.setLatitude(entity.getLatitude());
        vo.setSort(entity.getSort());
        vo.setStatus(entity.getStatus());
        vo.setCreatedTime(entity.getCreatedTime());
        return vo;
    }
}