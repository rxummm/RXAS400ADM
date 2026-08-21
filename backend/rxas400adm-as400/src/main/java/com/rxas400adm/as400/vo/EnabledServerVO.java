package com.rxas400adm.as400.vo;

/**
 * 启用的服务器最小视图（P1-5 加固）：登录页 AS400 模式下拉是免登录公开接口，
 * 只暴露 id / name / environment，不返回 host / username / port 等连接信息。
 */
public record EnabledServerVO(Long id, String name, String environment) {
}
