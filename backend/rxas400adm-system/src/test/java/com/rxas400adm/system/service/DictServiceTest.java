package com.rxas400adm.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.system.dto.DictItemDTO;
import com.rxas400adm.system.dto.DictTypeDTO;
import com.rxas400adm.system.entity.DictItem;
import com.rxas400adm.system.entity.DictType;
import com.rxas400adm.system.mapper.DictItemMapper;
import com.rxas400adm.system.mapper.DictTypeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DictServiceTest {

    @Mock
    private DictTypeMapper typeMapper;
    @Mock
    private DictItemMapper itemMapper;

    private DictService service;

    @BeforeEach
    void setUp() {
        service = new DictService(typeMapper, itemMapper);
    }

    @Test
    @DisplayName("新增字典类型 → 插入成功")
    void createType_shouldInsert() {
        when(typeMapper.insert(any(DictType.class))).thenReturn(1);

        DictTypeDTO dto = new DictTypeDTO();
        dto.setCode("GENDER");
        dto.setName("性别");
        dto.setSort(1);

        DictType created = service.createType(dto);

        assertEquals("GENDER", created.getCode());
        assertEquals("性别", created.getName());
        verify(typeMapper).insert(any(DictType.class));
    }

    @Test
    @DisplayName("新增字典类型 → code 重复则拒绝")
    void createType_duplicateCode_shouldThrow() {
        when(typeMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        DictTypeDTO dto = new DictTypeDTO();
        dto.setCode("GENDER");
        dto.setName("性别");

        assertThrows(BusinessException.class, () -> service.createType(dto));
    }

    @Test
    @DisplayName("新增字典项 → 插入成功")
    void createItem_shouldInsert() {
        when(itemMapper.insert(any(DictItem.class))).thenReturn(1);

        DictItemDTO dto = new DictItemDTO();
        dto.setTypeCode("GENDER");
        dto.setItemKey("M");
        dto.setItemValue("男");
        dto.setSort(1);

        DictItem created = service.createItem(dto);

        assertEquals("GENDER", created.getTypeCode());
        assertEquals("M", created.getItemKey());
        assertEquals("男", created.getItemValue());
        verify(itemMapper).insert(any(DictItem.class));
    }

    @Test
    @DisplayName("删除字典类型 → 同时清理子项")
    void deleteType_shouldCleanupItems() {
        when(typeMapper.selectById(1L)).thenReturn(new DictType());
        when(itemMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(3);
        when(typeMapper.deleteById(1L)).thenReturn(1);

        service.deleteType(1L);

        verify(itemMapper).delete(any(LambdaQueryWrapper.class));
        verify(typeMapper).deleteById(1L);
    }


}
