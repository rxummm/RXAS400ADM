package com.rxas400adm.as400;

/**
 * 多 AS400 环境客户端提供者。
 * 业务代码通过它获取当前请求上下文对应服务器的客户端，
 * 支持 mock（演示）与 JT400（生产）两种实现。
 */
public interface AS400ClientProvider {

    /** 当前请求上下文中的服务器客户端；未指定时回退到默认服务器 */
    AS400Client current();

    /** 指定服务器 ID 的客户端（生产模式下按服务器缓存 JT400 连接） */
    AS400Client forServer(Long serverId);

    /**
     * 使指定服务器的客户端连接与配置缓存失效（S2）：
     * 服务器主机/账号/密码/启停变更后调用，避免继续使用旧凭据直到重启。
     */
    void evict(Long serverId);
}
