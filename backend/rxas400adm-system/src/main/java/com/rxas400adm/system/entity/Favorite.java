package com.rxas400adm.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 快捷收藏（rx_favorite）：按用户收藏菜单/页面，侧边栏收藏列表 + 顶栏星标。
 */
@Data
@TableName("rx_favorite")
public class Favorite {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    /** 名称（i18n key 或文本） */
    private String title;

    /** 跳转路径（如 /jobs） */
    private String path;

    /** 图标（EP 组件名或 fa- 全名） */
    private String icon;

    private LocalDateTime createdTime;
}
