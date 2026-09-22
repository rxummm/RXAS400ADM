package com.rxas400adm.tpm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.common.util.EntityUtil;
import com.rxas400adm.tpm.entity.Equipment;
import com.rxas400adm.tpm.mapper.EquipmentMapper;
import com.rxas400adm.tpm.vo.EquipmentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class EquipmentService {

    private final EquipmentMapper mapper;

    public PageResult<EquipmentVO> pageQuery(String cono, String status, String location,
                                              String department, int current, int size) {
        LambdaQueryWrapper<Equipment> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(cono)) wrapper.eq(Equipment::getCono, cono);
        if (StringUtils.hasText(status)) wrapper.eq(Equipment::getStatus, status);
        if (StringUtils.hasText(location)) wrapper.like(Equipment::getLocation, location);
        if (StringUtils.hasText(department)) wrapper.eq(Equipment::getDepartment, department);
        wrapper.orderByDesc(Equipment::getCreatedTime);

        Page<Equipment> page = mapper.selectPage(new Page<>(current, size), wrapper);
        return new PageResult<>(page.getTotal(), page.getRecords().stream()
                .map(EquipmentVO::from).toList());
    }

    public Equipment getById(Long id) {
        return EntityUtil.require(id, "Equipment", mapper::selectById);
    }
}
