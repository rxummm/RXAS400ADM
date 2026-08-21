package com.rxas400adm.as400;

/**
 * 命令域：CL 命令执行、连接测试与资源释放。
 */
public interface CommandClient {

    String name();

    /** 执行 CL 命令（CRTBNDRPG、SAVOBJ、RSTOBJ、WRKACTJOB 等） */
    CommandResult execute(String command);

    /** 测试连接 */
    CommandResult testConnection();

    /**
     * 释放底层资源（P1：JT400 连接复用后的清理钩子）。
     * 服务器配置变更/删除时由 Provider evict 调用；Mock 实现无需处理（默认空实现）。
     */
    default void disconnect() {
    }
}
