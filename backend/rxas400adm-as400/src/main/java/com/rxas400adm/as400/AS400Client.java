package com.rxas400adm.as400;

/**
 * IBM i 访问抽象层（按域拆分后的复合接口）。
 *
 * <p>37 个方法按业务域拆分为子接口（M2 重构）：
 * <ul>
 *   <li>{@link CommandClient} —— CL 命令执行 / 连接测试 / 资源释放</li>
 *   <li>{@link SqlClient}     —— QSYS2 通用 SQL（动态列，Map 返回）</li>
 *   <li>{@link AuthClient}    —— AS400 用户凭据认证 / profile 查询</li>
 *   <li>{@link SourceClient}  —— 库 / 源文件 / 源成员</li>
 *   <li>{@link ObjectClient}  —— 对象搜索 / 详情 / 引用 / 权限 / 拓扑</li>
 *   <li>{@link IfsClient}     —— IFS 目录 / 文件读写 / 回收站</li>
 *   <li>{@link JobClient}     —— 作业队列 / SPOOL / SLA / 依赖图</li>
 *   <li>{@link SubsystemClient} —— 子系统状态与启停</li>
 *   <li>{@link SysvalClient}  —— 系统值</li>
 *   <li>{@link MessageFileClient} —— 消息文件（*MSGF）</li>
 *   <li>{@link PfClient}      —— PF 物理文件</li>
 * </ul>
 *
 * 业务代码不直接操作 JT400，只依赖本接口；
 * 由 MockAS400Client（开发/演示）或 JTOpenAS400Client（生产）实现。
 * 服务可按需收敛到子接口类型（如 IfsService 依赖 {@link IfsClient}）。
 *
 * <p>M2：子接口中尚未接入真实实现的查询一律用 {@code default} 方法优雅降级
 * （返回空集合/空字符串，如 {@link SourceClient}），实现类只覆写已接入的部分，
 * 新增方法时无需同时改动两个实现类。
 */
public interface AS400Client extends CommandClient, SqlClient, AuthClient, SourceClient,
        ObjectClient, IfsClient, JobClient, SubsystemClient, SysvalClient,
        MessageFileClient, PfClient {
}
