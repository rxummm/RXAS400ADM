package com.rxas400adm.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.system.dto.DictItemDTO;
import com.rxas400adm.system.dto.DictTypeDTO;
import com.rxas400adm.system.entity.DictItem;
import com.rxas400adm.system.entity.DictType;
import com.rxas400adm.system.mapper.DictItemMapper;
import com.rxas400adm.system.mapper.DictTypeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据字典（rx_dict_type / rx_dict_item）：类型 + 字典项两级 CRUD，
 * 供页面 el-select 引用，减少状态/类型字段硬编码。
 */
@Service
@RequiredArgsConstructor
public class DictService implements IDictService {

    private final DictTypeMapper typeMapper;
    private final DictItemMapper itemMapper;

    // ==================== 类型 ====================
    public List<DictType> listTypes() {
        return typeMapper.selectList(new LambdaQueryWrapper<DictType>()
                .orderByAsc(DictType::getSort));
    }

    
    public DictType createType(DictTypeDTO dto) {
        DictType type = new DictType();
        type.setCode(dto.getCode());
        type.setName(dto.getName());
        type.setRemark(dto.getRemark());
        type.setSort(dto.getSort());
        type.setStatus(dto.getStatus());
        long exists = typeMapper.selectCount(new LambdaQueryWrapper<DictType>()
                .eq(DictType::getCode, type.getCode()));
        if (exists > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Dict type code already exists: " + type.getCode());
        }
        if (type.getSort() == null) type.setSort(0);
        if (type.getStatus() == null) type.setStatus(1);
        type.setId(null);
        type.setCreatedTime(LocalDateTime.now());
        type.setUpdatedTime(LocalDateTime.now());
        typeMapper.insert(type);
        return type;
    }

    
    public DictType updateType(Long id, DictTypeDTO dto) {
        DictType type = typeMapper.selectById(id);
        if (type == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Dict type not found: " + id);
        }
        if (dto.getName() != null) type.setName(dto.getName());
        if (dto.getRemark() != null) type.setRemark(dto.getRemark());
        if (dto.getSort() != null) type.setSort(dto.getSort());
        if (dto.getStatus() != null) type.setStatus(dto.getStatus());
        type.setUpdatedTime(LocalDateTime.now());
        typeMapper.updateById(type);
        return type;
    }

    
    public void deleteType(Long id) {
        DictType type = typeMapper.selectById(id);
        if (type == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Dict type not found: " + id);
        }
        itemMapper.delete(new LambdaQueryWrapper<DictItem>().eq(DictItem::getTypeCode, type.getCode()));
        typeMapper.deleteById(id);
    }

    // ==================== 字典项 ====================
    public List<DictItem> listItems(String typeCode) {
        return itemMapper.selectList(new LambdaQueryWrapper<DictItem>()
                .eq(DictItem::getTypeCode, typeCode)
                .orderByAsc(DictItem::getSort));
    }

    /** 启用中的字典项（下拉引用） */
    public List<DictItem> enabledItems(String typeCode) {
        return itemMapper.selectList(new LambdaQueryWrapper<DictItem>()
                .eq(DictItem::getTypeCode, typeCode)
                .eq(DictItem::getStatus, 1)
                .orderByAsc(DictItem::getSort));
    }

    
    public DictItem createItem(DictItemDTO dto) {
        DictItem item = new DictItem();
        item.setTypeCode(dto.getTypeCode());
        item.setItemKey(dto.getItemKey());
        item.setItemValue(dto.getItemValue());
        item.setSort(dto.getSort());
        item.setStatus(dto.getStatus());
        long exists = itemMapper.selectCount(new LambdaQueryWrapper<DictItem>()
                .eq(DictItem::getTypeCode, item.getTypeCode())
                .eq(DictItem::getItemKey, item.getItemKey()));
        if (exists > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Dict item already exists: " + item.getItemKey());
        }
        if (item.getSort() == null) item.setSort(0);
        if (item.getStatus() == null) item.setStatus(1);
        item.setId(null);
        item.setCreatedTime(LocalDateTime.now());
        item.setUpdatedTime(LocalDateTime.now());
        itemMapper.insert(item);
        return item;
    }

    
    public DictItem updateItem(Long id, DictItemDTO dto) {
        DictItem item = itemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Dict item not found: " + id);
        }
        if (dto.getItemValue() != null) item.setItemValue(dto.getItemValue());
        if (dto.getSort() != null) item.setSort(dto.getSort());
        if (dto.getStatus() != null) item.setStatus(dto.getStatus());
        item.setUpdatedTime(LocalDateTime.now());
        itemMapper.updateById(item);
        return item;
    }

    
    public void deleteItem(Long id) {
        if (itemMapper.selectById(id) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Dict item not found: " + id);
        }
        itemMapper.deleteById(id);
    }
}