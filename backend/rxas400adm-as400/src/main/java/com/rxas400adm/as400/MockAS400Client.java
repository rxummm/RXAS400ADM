package com.rxas400adm.as400;

/**
 * 演示/开发用的 Mock 客户端。
 * <p>
 * 委托样板收编至 {@link AbstractDelegatingAs400Client}（中-15/19），本类只保留
 * Mock 子客户端装配与 SourceClient 渐进实现；共享仿真状态集中在 MockState。
 */
public class MockAS400Client extends AbstractDelegatingAs400Client {

    private final MockState state;
    private final MockCommandClient commandClient;
    private final MockSqlClient sqlClient;
    private final MockAuthClient authClient;
    private final MockObjectClient objectClient;
    private final MockIfsClient ifsClient;
    private final MockJobClient jobClient;
    private final MockSubsystemClient subsystemClient;
    private final MockSysvalClient sysvalClient;
    private final MockMessageFileClient messageFileClient;
    private final MockPfClient pfClient;
    private final MockSourceClient sourceClient;

    public MockAS400Client() {
        this("MOCK");
    }

    public MockAS400Client(String serverName) {
        this.state = new MockState(serverName);
        this.commandClient = new MockCommandClient(state);
        this.sqlClient = new MockSqlClient(state);
        this.authClient = new MockAuthClient(state);
        this.objectClient = new MockObjectClient(state);
        this.ifsClient = new MockIfsClient(state);
        this.jobClient = new MockJobClient(state);
        this.subsystemClient = new MockSubsystemClient(state);
        this.sysvalClient = new MockSysvalClient(state);
        this.messageFileClient = new MockMessageFileClient(state);
        this.pfClient = new MockPfClient(state);
        this.sourceClient = new MockSourceClient();
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
