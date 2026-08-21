package com.rxas400adm.as400;

import com.rxas400adm.as400.model.SysvalRow;

import java.util.ArrayList;
import java.util.List;

/**
 * Mock SysvalClient 委托实现（系统值查询与修改）。
 */
class MockSysvalClient implements SysvalClient {

    private final MockState state;

    MockSysvalClient(MockState state) {
        this.state = state;
    }

    @Override
    public List<SysvalRow> listSystemValues() {
        String[][] defs = {
                {"QCCSID", "国家/地区标识符（CCSID）"},
                {"QTIME", "系统时间"},
                {"QDATE", "系统日期"},
                {"QHOUR", "系统时间格式（12/24 小时）"},
                {"QDATFMT", "系统日期格式"},
                {"QMAXSIGN", "最大无效登录尝试次数"},
                {"QINACTITV", "交互作业不活动时间限制（分钟）"},
                {"QINACTMSGQ", "不活动作业处理方式"},
                {"QPWRDWNLMT", "电源关闭时间限制（分钟）"},
                {"QALWOBJRST", "允许恢复的对象"},
                {"QJOBMSGQMX", "作业消息队列最大消息数"},
                {"QSTGLOWACN", "存储低于阈值时的处理动作"},
                {"QSTGLOWLMT", "辅助存储低限制百分比"},
                {"QMLTTHDAC", "多线程作业最大活动线程数"},
                {"QPFXPGM", "用户前缀程序"},
                {"QCTLSBSD", "控制系统子系统"},
                {"QFRCCVNRST", "强制转换恢复"},
                {"QSRLSTLST", "保存/恢复活动列表"}
        };
        List<SysvalRow> rows = new ArrayList<>();
        for (String[] d : defs) {
            rows.add(new SysvalRow(d[0], state.sysvalValues.getOrDefault(d[0], ""), d[1], "CHAR"));
        }
        return rows;
    }

    @Override
    public CommandResult changeSystemValue(String name, String value) {
        if (name == null || name.isBlank() || value == null) {
            return CommandResult.fail("系统值名称与值不能为空");
        }
        String upper = name.trim().toUpperCase();
        if (!state.sysvalValues.containsKey(upper)) {
            return CommandResult.fail("未知系统值: " + upper);
        }
        state.sysvalValues.put(upper, value.trim());
        return CommandResult.ok("模拟修改系统值 " + upper + " = " + value.trim());
    }
}