package com.rxas400adm.as400;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400Message;
import com.ibm.as400.access.CommandCall;
import lombok.extern.slf4j.Slf4j;

/**
 * JTOpen CommandClient 委托实现（CL 命令执行 / 连接测试 / 资源释放）。
 */
@Slf4j
class JTOpenCommandClient implements CommandClient {

    private final JTOpenConnectionState state;

    JTOpenCommandClient(JTOpenConnectionState state) {
        this.state = state;
    }

    @Override
    public String name() {
        return state.name();
    }

    @Override
    public CommandResult execute(String command) {
        AS400 system = state.connect();
        try {
            CommandCall call = new CommandCall(system);
            boolean ok = call.run(command);
            StringBuilder sb = new StringBuilder();
            for (AS400Message msg : call.getMessageList()) {
                sb.append(msg.getText()).append('\n');
            }
            return ok ? CommandResult.ok(sb.toString().isBlank() ? "执行成功" : sb.toString())
                    : CommandResult.fail(sb.toString().isBlank() ? "执行失败" : sb.toString());
        } catch (Exception e) {
            state.invalidate(system);
            log.warn("IBM i 命令执行失败(host={}): {}", state.host, state.redact(command), e);
            return CommandResult.fail("命令执行失败: " + state.redact(e.getMessage()));
        }
    }

    @Override
    public CommandResult testConnection() {
        AS400 system = state.connect();
        try {
            system.validateSignon();
            return CommandResult.ok("JT400 连接成功: " + state.host);
        } catch (Exception e) {
            state.invalidate(system);
            return CommandResult.fail("连接失败: " + state.redact(e.getMessage()));
        }
    }

    @Override
    public void disconnect() {
        state.disconnect();
    }
}