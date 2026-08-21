package com.rxas400adm.system.vo;

/**
 * 收藏切换结果（FavoriteController.toggle 返回）。
 */
public record FavoriteToggleVO(boolean favorited, Long id) {
}
