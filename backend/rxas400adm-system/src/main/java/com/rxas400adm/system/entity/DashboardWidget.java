package com.rxas400adm.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 仪表盘 Widget 显示偏好（rx_dashboard_widget）：按用户记录各 widget 显隐。
 */
@Data
@TableName("rx_dashboard_widget")
public class DashboardWidget {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    /** widget 标识：cpu/mem/traffic/alert/task */
    private String widgetKey;

    /** 1=显示 0=隐藏 */
    private Integer enabled;
}