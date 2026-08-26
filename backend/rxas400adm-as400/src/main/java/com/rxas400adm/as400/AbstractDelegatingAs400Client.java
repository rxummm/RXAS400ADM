package com.rxas400adm.as400;

import com.rxas400adm.as400.model.AuthorityRow;
import com.rxas400adm.as400.model.GraphData;
import com.rxas400adm.as400.model.IfsEntry;
import com.rxas400adm.as400.model.JobQueueRow;
import com.rxas400adm.as400.model.JobSlaExecRow;
import com.rxas400adm.as400.model.MessageDescriptor;
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
 * AS400Client 委托门面公共基类（中-15/19）：
 * MockAS400Client 与 JTOpenAS400Client 原本各持一套子客户端字段并逐字复制约 45 个
 * 一行委托方法；本基类以模板方法收编——子类只需提供各子接口实例（accessor）
 * 与各自差异化的构造/连接语义。
 * SourceClient 为渐进实现接口（default 静默空实现），由子类按需覆盖，不在本基类收编。
 */
public abstract class AbstractDelegatingAs400Client implements AS400Client {

    protected abstract CommandClient commandClient();

    protected abstract SqlClient sqlClient();

    protected abstract AuthClient authClient();

    protected abstract ObjectClient objectClient();

    protected abstract IfsClient ifsClient();

    protected abstract JobClient jobClient();

    protected abstract SubsystemClient subsystemClient();

    protected abstract SysvalClient sysvalClient();

    protected abstract MessageFileClient messageFileClient();

    protected abstract PfClient pfClient();

    protected abstract SourceClient sourceClient();

    // ==================== CommandClient ====================

    @Override
    public String name() {
        return commandClient().name();
    }

    @Override
    public CommandResult execute(String command) {
        return commandClient().execute(command);
    }

    @Override
    public CommandResult testConnection() {
        return commandClient().testConnection();
    }

    @Override
    public void disconnect() {
        commandClient().disconnect();
    }

    // ==================== SqlClient ====================

    @Override
    public Map<String, Object> querySingle(String sql) {
        return sqlClient().querySingle(sql);
    }

    @Override
    public List<Map<String, Object>> queryList(String sql) {
        return sqlClient().queryList(sql);
    }

    @Override
    public List<Map<String, Object>> queryList(String sql, Object... params) {
        return sqlClient().queryList(sql, params);
    }

    @Override
    public List<Map<String, Object>> queryListChecked(String sql, Object... params) {
        return sqlClient().queryListChecked(sql, params);
    }

    /** S7：显式转发带行数上限的变体（否则接口 default 会退化为无上限路径） */
    @Override
    public List<Map<String, Object>> queryListCheckedBounded(String sql, int maxRows, Object... params) {
        return sqlClient().queryListCheckedBounded(sql, maxRows, params);
    }

    // ==================== AuthClient ====================

    @Override
    public boolean authenticate(String username, String password) {
        return authClient().authenticate(username, password);
    }

    @Override
    public UserProfileRow userProfile(String username) {
        return authClient().userProfile(username);
    }

    // ==================== ObjectClient ====================

    @Override
    public boolean objectExists(String library, String object) {
        return objectClient().objectExists(library, object);
    }

    @Override
    public List<ObjectRow> searchObjects(String library, String objectType) {
        return objectClient().searchObjects(library, objectType);
    }

    @Override
    public ObjectDetail objectDetail(String library, String objectName) {
        return objectClient().objectDetail(library, objectName);
    }

    @Override
    public List<ObjectRefRow> objectReferences(String library, String objectName, String direction) {
        return objectClient().objectReferences(library, objectName, direction);
    }

    @Override
    public List<AuthorityRow> objectAuthorities(String library, String objectName) {
        return objectClient().objectAuthorities(library, objectName);
    }

    @Override
    public GraphData objectGraph(String library) {
        return objectClient().objectGraph(library);
    }

    // ==================== IfsClient ====================

    @Override
    public List<IfsEntry> listIfsDir(String path) {
        return ifsClient().listIfsDir(path);
    }

    @Override
    public String readIfsFile(String path) {
        return ifsClient().readIfsFile(path);
    }

    @Override
    public boolean writeIfsFile(String path, String content) {
        return ifsClient().writeIfsFile(path, content);
    }

    @Override
    public boolean writeIfsFileBytes(String path, byte[] content) {
        return ifsClient().writeIfsFileBytes(path, content);
    }

    @Override
    public boolean mkdirIfs(String path) {
        return ifsClient().mkdirIfs(path);
    }

    @Override
    public String trashIfsFile(String path) {
        return ifsClient().trashIfsFile(path);
    }

    @Override
    public boolean restoreIfsFile(String trashPath) {
        return ifsClient().restoreIfsFile(trashPath);
    }

    @Override
    public byte[] readIfsFileBytes(String path) {
        return ifsClient().readIfsFileBytes(path);
    }

    @Override
    public InputStream readIfsFileStream(String path) {
        return ifsClient().readIfsFileStream(path);
    }

    // ==================== JobClient ====================

    @Override
    public List<JobQueueRow> listJobQueues() {
        return jobClient().listJobQueues();
    }

    @Override
    public List<SpoolRow> listSpoolFiles(String jobName, String jobUser, String jobNumber) {
        return jobClient().listSpoolFiles(jobName, jobUser, jobNumber);
    }

    @Override
    public List<JobSlaExecRow> jobSlaExecutions() {
        return jobClient().jobSlaExecutions();
    }

    @Override
    public GraphData jobDependencies() {
        return jobClient().jobDependencies();
    }

    // ==================== SubsystemClient ====================

    @Override
    public List<SubsystemRow> listSubsystems() {
        return subsystemClient().listSubsystems();
    }

    @Override
    public CommandResult startSubsystem(String name) {
        return subsystemClient().startSubsystem(name);
    }

    @Override
    public CommandResult endSubsystem(String name) {
        return subsystemClient().endSubsystem(name);
    }

    // ==================== SysvalClient ====================

    @Override
    public List<SysvalRow> listSystemValues() {
        return sysvalClient().listSystemValues();
    }

    @Override
    public CommandResult changeSystemValue(String name, String value) {
        return sysvalClient().changeSystemValue(name, value);
    }

    // ==================== MessageFileClient ====================

    @Override
    public List<MessageFileRow> listMessageFiles(String library) {
        return messageFileClient().listMessageFiles(library);
    }

    @Override
    public List<MessageRow> listMessages(String library, String file, String keyword) {
        return messageFileClient().listMessages(library, file, keyword);
    }

    @Override
    public CommandResult addMessage(MessageDescriptor msg) {
        return messageFileClient().addMessage(msg);
    }

    @Override
    public CommandResult updateMessage(MessageDescriptor msg) {
        return messageFileClient().updateMessage(msg);
    }

    @Override
    public CommandResult deleteMessage(String library, String file, String id) {
        return messageFileClient().deleteMessage(library, file, id);
    }

    // ==================== PfClient ====================

    @Override
    public List<PfRow> listPfFiles(String library) {
        return pfClient().listPfFiles(library);
    }

    @Override
    public List<PfColumnRow> pfColumns(String library, String file) {
        return pfClient().pfColumns(library, file);
    }

    @Override
    public List<Map<String, Object>> pfData(String library, String file, int limit) {
        return pfClient().pfData(library, file, limit);
    }

    // ==================== SourceClient ====================

    @Override
    public List<String> listLibraries() {
        return sourceClient().listLibraries();
    }

    @Override
    public List<String> listSourceFiles(String library) {
        return sourceClient().listSourceFiles(library);
    }

    @Override
    public List<String> listMembers(String library, String sourceFile) {
        return sourceClient().listMembers(library, sourceFile);
    }

    @Override
    public String readMember(String library, String sourceFile, String member) {
        return sourceClient().readMember(library, sourceFile, member);
    }
}
