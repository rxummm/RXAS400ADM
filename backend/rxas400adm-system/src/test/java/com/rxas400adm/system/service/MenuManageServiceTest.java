package com.rxas400adm.system.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.system.dto.SysMenuDTO;
import com.rxas400adm.system.entity.SysMenu;
import com.rxas400adm.system.mapper.SysMenuMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MenuManageServiceTest {

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, SysMenu.class);
    }

    @Mock
    private SysMenuMapper menuMapper;

    private MenuManageService service;

    @BeforeEach
    void setUp() {
        service = new MenuManageService(menuMapper);
    }

    @Test
    @DisplayName("create → 新增菜单")
    void create_shouldInsert() {
        when(menuMapper.insert(any(SysMenu.class))).thenAnswer(inv -> {
            inv.getArgument(0, SysMenu.class).setId(1L);
            return 1;
        });

        SysMenuDTO dto = new SysMenuDTO();
        dto.setTitle("测试菜单");
        dto.setMenuName("test");
        dto.setMenuType(2);

        SysMenu result = service.create(dto);
        assertEquals("测试菜单", result.getTitle());
    }

    @Test
    @DisplayName("delete → 有子菜单抛异常")
    void delete_hasChildren_shouldThrow() {
        when(menuMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);

        assertThrows(BusinessException.class, () -> service.delete(1L));
    }

    @Test
    @DisplayName("delete → 无子菜单则删除")
    void delete_noChildren_shouldDelete() {
        when(menuMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        service.delete(1L);
        verify(menuMapper).deleteById(1L);
    }

    @Test
    @DisplayName("toggleStatus → 切换菜单状态")
    void toggleStatus_shouldUpdate() {
        SysMenu menu = new SysMenu();
        menu.setId(1L);
        menu.setStatus(1);
        when(menuMapper.selectById(1L)).thenReturn(menu);

        SysMenu result = service.toggleStatus(1L, 0);
        assertEquals(0, result.getStatus());
        verify(menuMapper).updateById(result);
    }
}