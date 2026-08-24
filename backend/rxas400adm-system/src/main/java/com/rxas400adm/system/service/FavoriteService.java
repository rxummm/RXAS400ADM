package com.rxas400adm.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rxas400adm.system.entity.Favorite;
import com.rxas400adm.system.mapper.FavoriteMapper;
import com.rxas400adm.system.vo.FavoriteVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
import java.util.List;

/**
 * 快捷收藏（rx_favorite）：按用户增删查；同一 path 幂等切换。
 */
@Service
@RequiredArgsConstructor
public class FavoriteService implements IFavoriteService {

    private final FavoriteMapper favoriteMapper;

    public List<FavoriteVO> mine(String username) {
        return favoriteMapper.selectList(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUsername, username)
                .orderByDesc(Favorite::getCreatedTime))
                .stream().map(FavoriteVO::from).toList();
    }

    /** 是否已收藏 */
    public boolean isFavorited(String username, String path) {
        return favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUsername, username)
                .eq(Favorite::getPath, path)) > 0;
    }

    /** 切换收藏状态（已收藏则取消，未收藏则添加），返回切换后状态 */
    
    public com.rxas400adm.system.vo.FavoriteToggleVO toggle(String username, String title, String path, String icon) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("收藏路径不能为空");
        }
        Favorite existing = favoriteMapper.selectOne(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUsername, username)
                .eq(Favorite::getPath, path));
        if (existing != null) {
            favoriteMapper.deleteById(existing.getId());
            return new com.rxas400adm.system.vo.FavoriteToggleVO(false, existing.getId());
        }
        Favorite fav = new Favorite();
        fav.setUsername(username);
        fav.setTitle(title == null || title.isBlank() ? path : title);
        fav.setPath(path);
        fav.setIcon(icon);
        fav.setCreatedTime(LocalDateTime.now());
        favoriteMapper.insert(fav);
        return new com.rxas400adm.system.vo.FavoriteToggleVO(true, fav.getId());
    }

    
    public void remove(String username, String path) {
        favoriteMapper.delete(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUsername, username)
                .eq(Favorite::getPath, path));
    }
}
