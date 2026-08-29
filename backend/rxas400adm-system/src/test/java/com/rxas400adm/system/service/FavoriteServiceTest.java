package com.rxas400adm.system.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.system.entity.Favorite;
import com.rxas400adm.system.mapper.FavoriteMapper;
import com.rxas400adm.system.vo.FavoriteToggleVO;
import com.rxas400adm.system.vo.FavoriteVO;
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
class FavoriteServiceTest {

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Favorite.class);
    }

    @Mock
    private FavoriteMapper favoriteMapper;

    private FavoriteService service;

    @BeforeEach
    void setUp() {
        service = new FavoriteService(favoriteMapper);
    }

    @Test
    @DisplayName("toggle → 未收藏则添加，返回 favorited=true")
    void toggle_notFavorited_shouldAdd() {
        when(favoriteMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(favoriteMapper.insert(any(Favorite.class))).thenAnswer(inv -> {
            inv.getArgument(0, Favorite.class).setId(1L);
            return 1;
        });

        FavoriteToggleVO result = service.toggle("admin", "仪表板", "/dashboard", "el-icon-monitor");
        assertTrue(result.favorited());
        verify(favoriteMapper).insert(any(Favorite.class));
    }

    @Test
    @DisplayName("toggle → 已收藏则取消，返回 favorited=false")
    void toggle_alreadyFavorited_shouldRemove() {
        Favorite existing = new Favorite();
        existing.setId(1L);
        when(favoriteMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        FavoriteToggleVO result = service.toggle("admin", "仪表板", "/dashboard", null);
        assertFalse(result.favorited());
        verify(favoriteMapper).deleteById(1L);
    }

    @Test
    @DisplayName("toggle → 空路径抛异常")
    void toggle_blankPath_shouldThrow() {
        assertThrows(BusinessException.class, () -> service.toggle("admin", "", "", null));
    }

    @Test
    @DisplayName("mine → 返回用户收藏列表")
    void mine_shouldReturnList() {
        when(favoriteMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        List<FavoriteVO> result = service.mine("admin");
        assertNotNull(result);
    }

    @Test
    @DisplayName("remove → 删除指定收藏")
    void remove_shouldDelete() {
        service.remove("admin", "/dashboard");
        verify(favoriteMapper).delete(any(LambdaQueryWrapper.class));
    }
}