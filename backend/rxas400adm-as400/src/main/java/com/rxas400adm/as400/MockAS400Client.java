package com.rxas400adm.as400;

import com.rxas400adm.as400.model.AuthorityRow;
import com.rxas400adm.as400.model.GraphData;
import com.rxas400adm.as400.model.IfsEntry;
import com.rxas400adm.as400.model.JobQueueRow;
import com.rxas400adm.as400.model.JobSlaExecRow;
import com.rxas400adm.as400.model.MessageFileRow;
import com.rxas400adm.as400.model.MessageRow;
import com.rxas400adm.as400.model.ObjectDetail;
import com.rxas400adm.as400.model.ObjectRefRow;
import com.rxas400adm.as400.model.ObjectRow;
import com.rxas400adm.as400.model.PfColumnRow;
import com.rxas400adm.as400.model.PfRow;
import com.rxas400adm.as400.model.SpoolRow;
import com.rxas400adm.as400.model.SubsystemRow;
import com.rxas400adm.as400.model.SysvalRow;
import com.rxas400adm.as400.model.UserProfileRow;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * 演示/开发用的 Mock 客户端。
 * <p>
 * 按子接口委托实现：MockCommandClient、MockSqlClient、MockAuthClient、
 * MockObjectClient、MockIfsClient、MockJobClient、MockSubsystemClient、
 * MockSysvalClient、MockMessageFileClient、MockPfClient、MockSourceClient。
 * 共享仿真状态集中在 MockState。
 */
public class MockAS400Client implements AS400Client {

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

    // ==================== CommandClient ====================

    @Override
    public String name() {
        return commandClient.name();
    }

    @Override
    public CommandResult execute(String command) {
        return commandClient.execute(command);
    }

    @Override
    public CommandResult testConnection() {
        return commandClient.testConnection();
    }

    @Override
    public void disconnect() {
        commandClient.disconnect();
    }

    // ==================== SqlClient ====================

    @Override
    public Map<String, Object> querySingle(String sql) {
        return sqlClient.querySingle(sql);
    }

    @Override
    public List<Map<String, Object>> queryList(String sql) {
        return sqlClient.queryList(sql);
    }

    @Override
    public List<Map<String, Object>> queryList(String sql, Object... params) {
        return sqlClient.queryList(sql, params);
    }

    @Override
    public List<Map<String, Object>> queryListChecked(String sql, Object... params) {
        return sqlClient.queryListChecked(sql, params);
    }

    // ==================== AuthClient ====================

    @Override
    public boolean authenticate(String username, String password) {
        return authClient.authenticate(username, password);
    }

    @Override
    public UserProfileRow userProfile(String username) {
        return authClient.userProfile(username);
    }

    // ==================== ObjectClient ====================

    @Override
    public boolean objectExists(String library, String object) {
        return objectClient.objectExists(library, object);
    }

    @Override
    public List<ObjectRow> searchObjects(String library, String objectType) {
        return objectClient.searchObjects(library, objectType);
    }

    @Override
    public ObjectDetail objectDetail(String library, String objectName) {
        return objectClient.objectDetail(library, objectName);
    }

    @Override
    public List<ObjectRefRow> objectReferences(String library, String objectName, String direction) {
        return objectClient.objectReferences(library, objectName, direction);
    }

    @Override
    public List<AuthorityRow> objectAuthorities(String library, String objectName) {
        return objectClient.objectAuthorities(library, objectName);
    }

    @Override
    public GraphData objectGraph(String library) {
        return objectClient.objectGraph(library);
    }

    // ==================== IfsClient ====================

    @Override
    public List<IfsEntry> listIfsDir(String path) {
        return ifsClient.listIfsDir(path);
    }

    @Override
    public String readIfsFile(String path) {
        return ifsClient.readIfsFile(path);
    }

    @Override
    public boolean writeIfsFile(String path, String content) {
        return ifsClient.writeIfsFile(path, content);
    }

    @Override
    public boolean writeIfsFileBytes(String path, byte[] content) {
        return ifsClient.writeIfsFileBytes(path, content);
    }

    @Override
    public boolean mkdirIfs(String path) {
        return ifsClient.mkdirIfs(path);
    }

    @Override
    public String trashIfsFile(String path) {
        return ifsClient.trashIfsFile(path);
    }

    @Override
    public boolean restoreIfsFile(String trashPath) {
        return ifsClient.restoreIfsFile(trashPath);
    }

    @Override
    public byte[] readIfsFileBytes(String path) {
        return ifsClient.readIfsFileBytes(path);
    }

    @Override
    public InputStream readIfsFileStream(String path) {
        return ifsClient.readIfsFileStream(path);
    }

    // ==================== JobClient ====================

    @Override
    public List<JobQueueRow> listJobQueues() {
        return jobClient.listJobQueues();
    }

    @Override
    public List<SpoolRow> listSpoolFiles(String jobName, String jobUser, String jobNumber) {
        return jobClient.listSpoolFiles(jobName, jobUser, jobNumber);
    }

    @Override
    public List<JobSlaExecRow> jobSlaExecutions() {
        return jobClient.jobSlaExecutions();
    }

    @Override
    public GraphData jobDependencies() {
        return jobClient.jobDependencies();
    }

    // ==================== SubsystemClient ====================

    @Override
    public List<SubsystemRow> listSubsystems() {
        return subsystemClient.listSubsystems();
    }

    @Override
    public CommandResult startSubsystem(String name) {
        return subsystemClient.startSubsystem(name);
    }

    @Override
    public CommandResult endSubsystem(String name) {
        return subsystemClient.endSubsystem(name);
    }

    // ==================== SysvalClient ====================

    @Override
    public List<SysvalRow> listSystemValues() {
        return sysvalClient.listSystemValues();
    }

    @Override
    public CommandResult changeSystemValue(String name, String value) {
        return sysvalClient.changeSystemValue(name, value);
    }

    // ==================== MessageFileClient ====================

    @Override
    public List<MessageFileRow> listMessageFiles(String library) {
        return messageFileClient.listMessageFiles(library);
    }

    @Override
    public List<MessageRow> listMessages(String library, String file, String keyword) {
        return messageFileClient.listMessages(library, file, keyword);
    }

    @Override
    public CommandResult addMessage(String library, String file, String id, String text,
                                    String secondLevel, int severity) {
        return messageFileClient.addMessage(library, file, id, text, secondLevel, severity);
    }

    @Override
    public CommandResult updateMessage(String library, String file, String id, String text,
                                       String secondLevel, int severity) {
        return messageFileClient.updateMessage(library, file, id, text, secondLevel, severity);
    }

    @Override
    public CommandResult deleteMessage(String library, String file, String id) {
        return messageFileClient.deleteMessage(library, file, id);
    }

    // ==================== PfClient ====================

    @Override
    public List<PfRow> listPfFiles(String library) {
        return pfClient.listPfFiles(library);
    }

    @Override
    public List<PfColumnRow> pfColumns(String library, String file) {
        return pfClient.pfColumns(library, file);
    }

    @Override
    public List<Map<String, Object>> pfData(String library, String file, int limit) {
        return pfClient.pfData(library, file, limit);
    }

    // ==================== SourceClient ====================

    @Override
    public List<String> listLibraries() {
        return sourceClient.listLibraries();
    }

    @Override
    public List<String> listSourceFiles(String library) {
        return sourceClient.listSourceFiles(library);
    }

    @Override
    public List<String> listMembers(String library, String sourceFile) {
        return sourceClient.listMembers(library, sourceFile);
    }

    @Override
    public String readMember(String library, String sourceFile, String member) {
        return sourceClient.readMember(library, sourceFile, member);
    }
}