package com.rxas400adm.as400;

import java.util.Set;

/**
 * Mock CommandClient 委托实现（CL 命令仿真 / 连接测试）。
 */
class MockCommandClient implements CommandClient {

    /** 视为「模拟执行成功」的命令动词前缀（中-8：替代 startsWith 长布尔链，便于扩充） */
    private static final Set<String> OK_VERB_PREFIXES = Set.of("CRT", "SAV", "RST", "END", "CHG");

    private final MockState state;

    MockCommandClient(MockState state) {
        this.state = state;
    }

    @Override
    public String name() {
        return state.name();
    }

    @Override
    public CommandResult execute(String command) {
        String cmd = command.trim().toUpperCase();
        if (OK_VERB_PREFIXES.stream().anyMatch(cmd::startsWith)) {
            return CommandResult.ok("模拟执行成功: " + command.split("\\s+")[0]);
        }
        return CommandResult.ok("模拟执行完成: " + command);
    }

    @Override
    public CommandResult testConnection() {
        return CommandResult.ok("Mock 连接成功 (演示模式)");
    }

    @Override
    public void disconnect() {
        // mock 无需释放资源
    }
}