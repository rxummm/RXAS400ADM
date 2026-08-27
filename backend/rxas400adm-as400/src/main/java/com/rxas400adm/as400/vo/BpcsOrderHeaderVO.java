package com.rxas400adm.as400.vo;

import java.util.List;

/**
 * 【AS400 业务增强·P1】客户订单头 + 进程时间轴聚合 VO。
 * <p>时间轴节点已按环境事实裁剪：CHSTS3/CHSTS4（发运释放/发运确认）在目标业务中恒为 0，不输出节点。
 */
public record BpcsOrderHeaderVO(
        String cono,
        String orno,
        String customerNo,
        String shipTo,
        String orderDate,
        String reqDate,
        java.math.BigDecimal totalAmount,
        int lineCount,
        /** 当前阶段下标（对应 timeline 数组最后一个 reached 节点） */
        int currentStageIndex,
        List<TimelineNodeVO> timeline,
        /** 冻结信息（本期预留恒空数组，字段名待环境确认——见设计文档 §10⑥） */
        List<HoldVO> holds,
        /** 原始状态透传（供高级用户核对）：chsts 五位 / hstat / hid(CH=在册,CZ=已关闭) */
        Raw raw) {

    public record TimelineNodeVO(
            String stageKey,
            String nameKey,
            boolean reached,
            String timestamp,
            boolean current) {
    }

    public record HoldVO(String type, String labelKey) {
    }

    public record Raw(String chsts, String hstat, String hid) {
    }
}
