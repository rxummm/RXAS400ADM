package com.rxas400adm.as400;

/**
 * 生产环境 JT400 实现。由 AS400ClientProvider 按服务器配置创建（每服务器一个实例）。
 * <p>
 * 委托样板收编至 {@link AbstractDelegatingAs400Client}（中-15/19），本类只保留
 * JTOpen 子客户端装配；共享连接状态集中在 JTOpenConnectionState。
 * SourceClient 暂未实现（走接口 default 静默空实现）——已知缺口，见分析报告 D 系列备注。
 */
public class JTOpenAS400Client extends AbstractDelegatingAs400Client {

    private final JTOpenConnectionState state;
    private final JTOpenCommandClient commandClient;
    private final JTOpenSqlClient sqlClient;
    private final JTOpenAuthClient authClient;
    private final JTOpenObjectClient objectClient;
    private final JTOpenIfsClient ifsClient;
    private final JTOpenJobClient jobClient;
    private final JTOpenSubsystemClient subsystemClient;
    private final JTOpenSysvalClient sysvalClient;
    private final JTOpenMessageFileClient messageFileClient;
    private final JTOpenPfClient pfClient;
    private final JTOpenSourceClient sourceClient;

    public JTOpenAS400Client(String host, String user, String password) {
        this.state = new JTOpenConnectionState(host, user, password);
        this.commandClient = new JTOpenCommandClient(state);
        this.sqlClient = new JTOpenSqlClient(state);
        this.authClient = new JTOpenAuthClient(state);
        this.objectClient = new JTOpenObjectClient(state);
        this.ifsClient = new JTOpenIfsClient(state);
        this.jobClient = new JTOpenJobClient(state);
        this.subsystemClient = new JTOpenSubsystemClient(state);
        this.sysvalClient = new JTOpenSysvalClient(state);
        this.messageFileClient = new JTOpenMessageFileClient(state);
        this.pfClient = new JTOpenPfClient(state);
        this.sourceClient = new JTOpenSourceClient(state);
    }

    @Override
    protected CommandClient commandClient() {
        return commandClient;
    }

    @Override
    protected SqlClient sqlClient() {
        return sqlClient;
    }

    @Override
    protected AuthClient authClient() {
        return authClient;
    }

    @Override
    protected ObjectClient objectClient() {
        return objectClient;
    }

    @Override
    protected IfsClient ifsClient() {
        return ifsClient;
    }

    @Override
    protected JobClient jobClient() {
        return jobClient;
    }

    @Override
    protected SubsystemClient subsystemClient() {
        return subsystemClient;
    }

    @Override
    protected SysvalClient sysvalClient() {
        return sysvalClient;
    }

    @Override
    protected MessageFileClient messageFileClient() {
        return messageFileClient;
    }

    @Override
    protected PfClient pfClient() {
        return pfClient;
    }

    @Override
    protected SourceClient sourceClient() {
        return sourceClient;
    }
}
