package com.rxas400adm.system.vo;

import com.rxas400adm.system.entity.Favorite;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 快捷收藏视图对象（B5：禁止直接返回 Entity）。
 */
@Data
public class FavoriteVO {

    private Long id;
    private String username;
    private String title;
    private String path;
    private String icon;
    private LocalDateTime createdTime;

    public static FavoriteVO from(Favorite entity) {
        FavoriteVO vo = new FavoriteVO();
        vo.setId(entity.getId());
        vo.setUsername(entity.getUsername());
        vo.setTitle(entity.getTitle());
        vo.setPath(entity.getPath());
        vo.setIcon(entity.getIcon());
        vo.setCreatedTime(entity.getCreatedTime());
        return vo;
    }
}
