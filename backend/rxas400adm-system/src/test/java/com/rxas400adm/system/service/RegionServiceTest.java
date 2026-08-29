package com.rxas400adm.system.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.system.dto.RegionDTO;
import com.rxas400adm.system.entity.Region;
import com.rxas400adm.system.mapper.RegionMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegionServiceTest {

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Region.class);
    }

    @Mock
    private RegionMapper regionMapper;

    private RegionService service;

    @BeforeEach
    void setUp() {
        service = new RegionService(regionMapper);
    }

    @Test
    @DisplayName("children → 空 parentCode 返回省级")
    void children_emptyParent_shouldReturnProvinces() {
        when(regionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        List<Region> result = service.children(null);
        assertNotNull(result);
    }

    @Test
    @DisplayName("create → code 重复抛异常")
    void create_duplicateCode_shouldThrow() {
        when(regionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        RegionDTO dto = new RegionDTO();
        dto.setCode("460000");
        dto.setName("海南省");
        assertThrows(BusinessException.class, () -> service.create(dto));
    }

    @Test
    @DisplayName("create → 正常新增")
    void create_valid_shouldInsert() {
        when(regionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(regionMapper.insert(any(Region.class))).thenAnswer(inv -> {
            inv.getArgument(0, Region.class).setId(1L);
            return 1;
        });

        RegionDTO dto = new RegionDTO();
        dto.setCode("460000");
        dto.setName("海南省");
        dto.setLevel(1);

        Region result = service.create(dto);
        assertEquals("460000", result.getCode());
        assertEquals("海南省", result.getName());
    }

    @Test
    @DisplayName("delete → 有下级数据抛异常")
    void delete_hasChildren_shouldThrow() {
        Region region = new Region();
        region.setId(1L);
        region.setCode("460000");
        when(regionMapper.selectById(1L)).thenReturn(region);
        when(regionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(3L);

        assertThrows(BusinessException.class, () -> service.delete(1L));
    }

    @Test
    @DisplayName("delete → 无下级数据则删除")
    void delete_noChildren_shouldDelete() {
        Region region = new Region();
        region.setId(1L);
        region.setCode("460000");
        when(regionMapper.selectById(1L)).thenReturn(region);
        when(regionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        service.delete(1L);
        verify(regionMapper).deleteById(1L);
    }

    @Test
    @DisplayName("page → 分页查询返回结果")
    void page_shouldReturnPageResult() {
        Page<Region> page = new Page<>(1, 10);
        page.setTotal(0);
        page.setRecords(List.of());
        when(regionMapper.selectPage(any(), any(LambdaQueryWrapper.class))).thenReturn(page);

        PageResult<Region> result = service.page(1, 10, null, null, null);
        assertNotNull(result);
    }

    @Test
    @DisplayName("search → 关键字搜索")
    void search_shouldCallMapper() {
        when(regionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        List<Region> result = service.search("海南", null);
        assertNotNull(result);
    }
}