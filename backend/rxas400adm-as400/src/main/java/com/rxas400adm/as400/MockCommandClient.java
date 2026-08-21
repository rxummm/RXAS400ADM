package com.rxas400adm.as400;

/**
 * Mock CommandClient 委托实现（CL 命令仿真 / 连接测试）。
 */
class MockCommandClient implements CommandClient {

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
        if (cmd.startsWith("CRT") || cmd.startsWith("SAV") || cmd.startsWith("RST")
                || cmd.startsWith("END") || cmd.startsWith("CHG")) {
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