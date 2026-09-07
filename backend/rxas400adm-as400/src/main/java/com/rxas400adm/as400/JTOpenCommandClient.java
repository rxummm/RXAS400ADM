package com.rxas400adm.as400;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400Message;
import com.ibm.as400.access.CommandCall;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Semaphore;

/**
 * JTOpen CommandClient 委托实现（CL 命令执行 / 连接测试 / 资源释放）。
 */
@Slf4j
class JTOpenCommandClient implements CommandClient {

    private static final int MAX_CONCURRENT_COMMANDS = 4;

    private final JTOpenConnectionState state;
    private final Semaphore commandSemaphore = new Semaphore(MAX_CONCURRENT_COMMANDS);

    JTOpenCommandClient(JTOpenConnectionState state) {
        this.state = state;
    }

    @Override
    public String name() {
        return state.name();
    }

    @Override
    public CommandResult execute(String command) {
        // B12：每服务器并发命令数流控，避免 CL 命令风暴拖垮 IBM i
        if (!commandSemaphore.tryAcquire()) {
            throw new BusinessException(ErrorCode.AS400_COMMAND_BUSY, "Command concurrency limit reached, please retry later");
        }
        try {
            AS400 system = state.connect();
            try {
                CommandCall call = new CommandCall(system);
                boolean ok = call.run(command);
                StringBuilder sb = new StringBuilder();
                for (AS400Message msg : call.getMessageList()) {
                    sb.append(msg.getText()).append('\n');
                }
                return ok ? CommandResult.ok(sb.toString().isBlank() ? "Command executed successfully" : sb.toString())
                        : CommandResult.fail(sb.toString().isBlank() ? "Command execution failed" : sb.toString());
            } catch (Exception e) {
                state.invalidate(system);
                log.warn("IBM i 命令执行失败(host={}): {}", state.host, state.redact(command), e);
                return CommandResult.fail("Command execution failed: " + state.redact(e.getMessage()));
            }
        } finally {
            commandSemaphore.release();
        }
    }

    @Override
    public CommandResult testConnection() {
        AS400 system = state.connect();
        try {
            system.validateSignon();
            return CommandResult.ok("JT400 connection successful: " + state.host);
        } catch (Exception e) {
            state.invalidate(system);
            return CommandResult.fail("Connection failed: " + state.redact(e.getMessage()));
        }
    }

    @Override
    public void disconnect() {
        state.disconnect();
    }
}