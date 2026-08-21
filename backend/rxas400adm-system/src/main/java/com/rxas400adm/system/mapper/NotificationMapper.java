package com.rxas400adm.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rxas400adm.system.entity.Notification;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface NotificationMapper extends BaseMapper<Notification> {

    /** 批量插入通知（公告/告警群发用，替代 N 次单条 INSERT）。SQL 已迁至 resources/mapper/NotificationMapper.xml */
    int insertBatch(@Param("list") List<Notification> notifications);
}