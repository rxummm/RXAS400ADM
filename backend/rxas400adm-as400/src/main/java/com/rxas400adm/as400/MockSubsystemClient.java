package com.rxas400adm.as400;

import com.rxas400adm.as400.model.SubsystemRow;

import java.util.ArrayList;
import java.util.List;

/**
 * Mock SubsystemClient 委托实现（子系统状态与启停）。
 */
class MockSubsystemClient implements SubsystemClient {

    private final MockState state;

    MockSubsystemClient(MockState state) {
        this.state = state;
    }

    @Override
    public List<SubsystemRow> listSubsystems() {
        String[][] systems = {
                {"QSYS", "系统基本子系统", "SYSBAS"},
                {"QINTER", "交互作业子系统", "QSYS"},
                {"QBATCH", "批处理作业子系统", "QSYS"},
                {"QCMDQ", "控制台消息队列子系统", "QSYS"},
                {"QSPL", "后台打印子系统", "QSYS"},
                {"QSYSPRT", "打印作业子系统", "QSYS"}
        };
        List<SubsystemRow> rows = new ArrayList<>();
        for (String[] s : systems) {
            String status = state.subsystemStatus.getOrDefault(s[0], "ACTIVE");
            rows.add(new SubsystemRow(s[0], s[1], status,
                    "ACTIVE".equals(status) ? 1 + MockState.RANDOM.nextInt(9) : 0L,
                    20L, s[2]));
        }
        return rows;
    }

    @Override
    public CommandResult startSubsystem(String name) {
        if (name == null || name.isBlank()) {
            return CommandResult.fail("子系统名不能为空");
        }
        state.subsystemStatus.put(name.trim().toUpperCase(), "ACTIVE");
        return CommandResult.ok("模拟启动子系统: " + name.trim().toUpperCase());
    }

    @Override
    public CommandResult endSubsystem(String name) {
        if (name == null || name.isBlank()) {
            return CommandResult.fail("子系统名不能为空");
        }
        state.subsystemStatus.put(name.trim().toUpperCase(), "INACTIVE");
        return CommandResult.ok("模拟停止子系统: " + name.trim().toUpperCase());
    }
}