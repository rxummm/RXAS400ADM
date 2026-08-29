package com.rxas400adm.system.service;

import com.rxas400adm.system.vo.FavoriteToggleVO;
import com.rxas400adm.system.vo.FavoriteVO;

import java.util.List;

/**
 * 快捷收藏服务接口（rx_favorite）。
 */
public interface IFavoriteService {

    List<FavoriteVO> mine(String username);

    boolean isFavorited(String username, String path);

    FavoriteToggleVO toggle(String username, String title, String path, String icon);

    void remove(String username, String path);
}
