package com.rxas400adm.system.service;

import com.rxas400adm.system.entity.Favorite;

import java.util.List;
import java.util.Map;

/**
 * 快捷收藏服务接口（rx_favorite）。
 */
public interface IFavoriteService {

    List<Favorite> mine(String username);

    boolean isFavorited(String username, String path);

    com.rxas400adm.system.vo.FavoriteToggleVO toggle(String username, String title, String path, String icon);

    void remove(String username, String path);
}
