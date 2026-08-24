package com.rxas400adm.as400;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import com.rxas400adm.as400.model.MessageDescriptor;
import com.rxas400adm.as400.model.GraphData;
import com.rxas400adm.as400.model.IfsEntry;
import com.rxas400adm.as400.model.ObjectRefRow;
import com.rxas400adm.as400.model.PfColumnRow;
import com.rxas400adm.as400.model.JobSlaExecRow;
import com.rxas400adm.as400.model.MessageRow;
import com.rxas400adm.as400.model.ObjectRow;
import com.rxas400adm.as400.model.SpoolRow;
import com.rxas400adm.as400.model.UserProfileRow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockAS400ClientTest {

    private final MockAS400Client client = new MockAS400Client();

    @Test
    void execute_shouldReturnSuccess() {
        CommandResult result = client.execute("CRTBNDRPG PGM(APP/ORDERMAINT)");
        assertTrue(result.success());
        assertTrue(result.message().contains("CRTBNDRPG"));
    }

    @Test
    void listLibraries_shouldContainSampleLibraries() {
        List<String> libraries = client.listLibraries();
        assertTrue(libraries.contains("APP"));
        assertTrue(libraries.contains("QTEMP"));
    }

    @Test
    void listMembers_shouldReturnRpgleMembers() {
        List<String> members = client.listMembers("APP", "QRPGLESRC");
        assertEquals(List.of("CUSTMAINT", "ORDERMAINT", "INVMOVE", "DAILYBAL"), members);
    }

    @Test
    void readMember_shouldContainSourceContent() {
        String content = client.readMember("APP", "QRPGLESRC", "CUSTMAINT");
        assertTrue(content.contains("CUSTMAINT"));
        assertTrue(content.contains("*ENTRY"));
    }

    @Test
    void querySingle_shouldReturnCpuValueInRange() {
        Object value = client.querySingle("SELECT CPU_UTILIZATION FROM QSYS2.SYSTEM_STATUS_INFO").get("CPU_UTILIZATION");
        assertTrue(value instanceof Number);
        double cpu = ((Number) value).doubleValue();
        assertTrue(cpu > 0 && cpu <= 100, "CPU 应在 0~100 区间，实际 " + cpu);
    }

    @Test
    void queryList_shouldReturnActiveJobsWithMsgw() {
        List<Map<String, Object>> jobs = client.queryList(
                "SELECT JOB_NAME, JOB_STATUS FROM TABLE(QSYS2.ACTIVE_JOB_INFO()) X");
        assertFalse(jobs.isEmpty());
        assertTrue(jobs.stream().anyMatch(j -> "MSGW".equals(j.get("JOB_STATUS"))));
    }

    @Test
    void queryList_shouldFilterMsgwOnly() {
        List<Map<String, Object>> jobs = client.queryList(
                "SELECT JOB_NAME FROM TABLE(QSYS2.ACTIVE_JOB_INFO()) X WHERE JOB_STATUS = 'MSGW'");
        assertFalse(jobs.isEmpty());
        assertTrue(jobs.stream().allMatch(j -> "MSGW".equals(j.get("JOB_STATUS"))));
    }

    @Test
    void queryList_shouldReturnAspNet() {
        List<Map<String, Object>> asps = client.queryList(
                "SELECT ASP_NAME, TOTAL_SPACE, USED_SPACE FROM QSYS2.ASP_INFO");
        assertFalse(asps.isEmpty());
        assertTrue(asps.stream().anyMatch(a -> "SYSBAS".equals(a.get("ASP_NAME"))));
    }

    @Test
    void queryList_joblog_shouldReturnRows() {
        List<Map<String, Object>> log = client.queryList(
                "SELECT ORDINAL_POSITION, MESSAGE_ID FROM TABLE(QSYS2.JOBLOG_INFO('JOB', 'QSECOFR/JOB1001', '*JOBLOG')) X");
        assertFalse(log.isEmpty());
        assertTrue(log.get(0).containsKey("ORDINAL_POSITION"));
    }

    @Test
    void queryList_msgQueue_shouldReturnInquiry() {
        List<Map<String, Object>> msgs = client.queryList(
                "SELECT MESSAGE_ID, REPLY_STATUS FROM TABLE(QSYS2.MESSAGE_QUEUE_INFO('*JOB', 'QSECOFR/JOB1001')) X");
        assertFalse(msgs.isEmpty());
        assertTrue(msgs.stream().anyMatch(m -> "MSGW".equals(m.get("REPLY_STATUS"))));
    }

    @Test
    void queryList_unknownSql_shouldReturnEmpty() {
        assertTrue(client.queryList("SELECT * FROM QSYS2.UNKNOWN_VIEW").isEmpty());
    }

    @Test
    void testConnection_shouldBeOk() {
        assertTrue(client.testConnection().success());
    }

    // ==================== SQL 分发：monitor 表 ====================

    @Test
    void queryList_subsystemInfo_shouldReturnRows() {
        List<Map<String, Object>> rows = client.queryList("SELECT * FROM QSYS2.SUBSYSTEM_INFO");
        assertEquals(4, rows.size());
        assertTrue(rows.get(0).containsKey("SUBSYSTEM_NAME"));
    }

    @Test
    void queryList_netstatInfo_shouldReturnRows() {
        List<Map<String, Object>> rows = client.queryList("SELECT * FROM QSYS2.NETSTAT_INFO");
        assertEquals(3, rows.size());
    }

    @Test
    void queryList_outputQueueInfo_shouldReturnRows() {
        List<Map<String, Object>> rows = client.queryList("SELECT * FROM QSYS2.OUTPUT_QUEUE_INFO");
        assertEquals(4, rows.size());
        assertTrue(rows.stream().anyMatch(r -> "HELD".equals(r.get("SPOOLED_FILE_STATUS"))));
    }

    @Test
    void queryList_activeJobs_lckwOnly_shouldFilter() {
        List<Map<String, Object>> rows = client.queryList(
                "SELECT * FROM QSYS2.ACTIVE_JOB_INFO() X WHERE JOB_STATUS = 'LCKW'");
        assertFalse(rows.isEmpty());
        assertTrue(rows.stream().allMatch(r -> "LCKW".equals(r.get("JOB_STATUS"))));
    }

    // ==================== P1-6 参数化 substitute 路径 ====================

    @Test
    void queryList_withParams_shouldSubstituteAndDispatch() {
        List<Map<String, Object>> rows = client.queryList(
                "SELECT * FROM QSYS2.ACTIVE_JOB_INFO() X WHERE JOB_STATUS = ?", "MSGW");
        assertFalse(rows.isEmpty());
        assertTrue(rows.stream().allMatch(r -> "MSGW".equals(r.get("JOB_STATUS"))));
    }

    @Test
    void queryList_withParams_noPlaceholder_shouldFallback() {
        List<Map<String, Object>> rows = client.queryList("SELECT * FROM QSYS2.ASP_INFO", "unused");
        assertEquals(2, rows.size());
    }

    // ==================== 业务表查询（mockSelectAll） ====================

    @Test
    void queryList_selectOrders_shouldReturn60Rows() {
        List<Map<String, Object>> rows = client.queryList("SELECT * FROM APP.ORDERS");
        assertEquals(60, rows.size());
        assertTrue(rows.get(0).containsKey("ORDER_NO"));
    }

    @Test
    void queryList_countOrders_shouldReturnCount() {
        List<Map<String, Object>> rows = client.queryList("SELECT COUNT(*) FROM APP.ORDERS");
        assertEquals(1, rows.size());
        assertEquals(60L, ((Number) rows.get(0).get("CNT")).longValue());
    }

    @Test
    void queryList_orders_offsetFetch_shouldPaginate() {
        List<Map<String, Object>> rows = client.queryList(
                "SELECT * FROM APP.ORDERS OFFSET 10 ROWS FETCH NEXT 5 ROWS ONLY");
        assertEquals(5, rows.size());
        assertEquals("SO000011", rows.get(0).get("ORDER_NO"));
    }

    @Test
    void queryList_orders_like_shouldFilter() {
        List<Map<String, Object>> rows = client.queryList(
                "SELECT * FROM APP.ORDERS WHERE CUSTOMER_NAME LIKE '%华东%'");
        assertFalse(rows.isEmpty());
        assertTrue(rows.size() < 60);
    }

    @Test
    void queryList_systables_like_shouldFilter() {
        List<Map<String, Object>> rows = client.queryList(
                "SELECT * FROM QSYS2.SYSTABLES WHERE TABLE_NAME LIKE '%ORD%'");
        assertTrue(rows.stream().anyMatch(r -> String.valueOf(r.get("TABLE_NAME")).contains("ORD")));
    }

    @Test
    void queryList_syscolumns_orders_shouldReturnFields() {
        List<Map<String, Object>> rows = client.queryList(
                "SELECT * FROM QSYS2.SYSCOLUMNS WHERE TABLE_NAME = 'ORDERS'");
        assertEquals(6, rows.size());
        assertEquals("ORDER_NO", rows.get(0).get("COLUMN_NAME"));
    }

    // ==================== querySingle 其他分支 ====================

    @Test
    void querySingle_memory_shouldReturnValue() {
        Map<String, Object> r = client.querySingle(
                "SELECT MAIN_STORAGE_USED_PERCENT FROM QSYS2.SYSTEM_STATUS_INFO");
        assertTrue(r.containsKey("MAIN_STORAGE_USED_PERCENT"));
    }

    @Test
    void querySingle_unknown_shouldReturnMock() {
        assertEquals("MOCK", client.querySingle("SELECT * FROM SOMETHING").get("RESULT"));
    }

    // ==================== 对象搜索 / 权限 ====================

    @Test
    void searchObjects_typeFilter_shouldFilter() {
        List<ObjectRow> rows = client.searchObjects("APP", "PGM");
        assertFalse(rows.isEmpty());
        assertTrue(rows.stream().allMatch(r -> "PGM".equals(r.type())));
    }

    @Test
    void objectDetail_blankName_shouldReturnNull() {
        assertNull(client.objectDetail("APP", " "));
    }

    @Test
    void objectAuthorities_blankName_shouldReturnEmpty() {
        assertTrue(client.objectAuthorities("APP", " ").isEmpty());
    }

    @Test
    void objectExists_QStarLibsAndBlank_shouldReturnFalse() {
        assertTrue(client.objectExists("APP", "ORDERMAINT"));
        assertTrue(client.objectExists("app", "ORDERMAINT"));
        assertFalse(client.objectExists("QTEMP", "X"));
        assertFalse(client.objectExists("QSYS", "X"));
        assertFalse(client.objectExists("", "X"));
        assertFalse(client.objectExists("  ", "X"));
        assertFalse(client.objectExists(null, "X"));
    }

    @Test
    void objectReferences_inAndOutDirections_shouldDiffer() {
        // IN：被谁引用（3 行，第三行 refName 为目标对象）
        List<ObjectRefRow> in = client.objectReferences("APP", "PAYROLL", "IN");
        assertEquals(3, in.size());
        assertEquals("DAILYBAL", in.get(0).name());
        assertTrue(in.stream().allMatch(r -> "APP".equals(r.library())));
        assertTrue(in.stream().anyMatch(r -> "PAYROLL".equals(r.refName())));
        // OUT：引用了什么（4 行：PGM/FILE/FILE/MSGF）
        List<ObjectRefRow> out = client.objectReferences("APP", "INVCTL", "OUT");
        assertEquals(4, out.size());
        assertEquals("INVCTL", out.get(0).name());
        assertTrue(out.stream().anyMatch(r -> "MSGDTA".equals(r.refName()) && "MSGF".equals(r.refType())));
        // direction 大小写不敏感 / 空值回退 OUT
        assertEquals(4, client.objectReferences("APP", "INVCTL", "out").size());
        assertEquals(4, client.objectReferences("APP", "INVCTL", null).size());
    }

    @Test
    void pfColumns_custmastAndDefault_shouldDiffer() {
        List<PfColumnRow> cust = client.pfColumns("APP", "CUSTMAST");
        assertEquals(3, cust.size());
        assertEquals("CUSTNO", cust.get(0).name());
        assertEquals("NUMERIC", cust.get(0).type());
        List<PfColumnRow> other = client.pfColumns("APP", "ORDERS");
        assertEquals(2, other.size());
        assertEquals("ORDNO", other.get(0).name());
        assertEquals("STATUS", other.get(1).name());
        // null/小写都走 default 分支
        assertEquals(2, client.pfColumns("APP", null).size());
        assertEquals(3, client.pfColumns("APP", "custmast").size());
    }

    @Test
    void pfData_branchAndLimitClamp() {
        List<Map<String, Object>> cust = client.pfData("APP", "CUSTMAST", 5);
        assertEquals(5, cust.size());
        assertEquals(10001, cust.get(0).get("CUSTNO"));
        List<Map<String, Object>> other = client.pfData("APP", "ORDERS", 3);
        assertEquals(3, other.size());
        assertEquals(9001, other.get(0).get("ORDNO"));
        // limit 钳制：<=0 → 1 行，>100 → 100 行
        assertEquals(1, client.pfData("APP", "ORDERS", 0).size());
        assertEquals(1, client.pfData("APP", "ORDERS", -5).size());
        assertEquals(100, client.pfData("APP", "ORDERS", 1000).size());
        assertEquals(1, client.pfData("APP", null, 1).size());
    }

    // ==================== IFS 内置文件内容 switch（readme/guide 等） ====================

    @Test
    void ifs_readBuiltinFiles_shouldMatchByFileName() {
        assertTrue(client.readIfsFile("/QOpenSys/rxas400/readme.txt").contains("部署说明"));
        assertTrue(client.readIfsFile("/QOpenSys/rxas400/docs/operation_guide.md").contains("运维手册"));
        assertTrue(client.readIfsFile("/QOpenSys/rxas400/backup_plan.txt").contains("备份计划"));
        assertTrue(client.readIfsFile("/QOpenSys/rxas400/logs/joblog_20260812.log").contains("BATCH01"));
        // 未知文件 → 默认内容含文件名
        String unknown = client.readIfsFile("/QOpenSys/rxas400/custom.txt");
        assertTrue(unknown.contains("custom.txt"));
        // 反斜杠路径归一化 + 子目录匹配
        assertTrue(client.readIfsFile("\\QOpenSys\\rxas400\\readme.txt").contains("部署说明"));
        // null 路径 → 文件名空 → 默认分支
        assertNotNull(client.readIfsFile(null));
    }

    // ==================== IFS 写入/删除/还原（有状态核心链路） ====================

    @Test
    void ifs_writeReadTrashRestore_roundTrip() {
        assertTrue(client.writeIfsFile("/QOpenSys/rxas400/document/hello.txt", "hello world"));
        assertEquals("hello world", client.readIfsFile("/QOpenSys/rxas400/document/hello.txt"));
        List<IfsEntry> entries = client.listIfsDir("/QOpenSys/rxas400/document");
        assertTrue(entries.stream().anyMatch(e -> "hello.txt".equals(e.name())));
        String trashPath = client.trashIfsFile("/QOpenSys/rxas400/document/hello.txt");
        assertNotNull(trashPath);
        assertTrue(trashPath.startsWith("/QOpenSys/rxas400/temp/as400histdocs"));
        assertTrue(client.readIfsFile("/QOpenSys/rxas400/document/hello.txt").contains("模拟文件内容"));
        List<IfsEntry> trash = client.listIfsDir(
                "/QOpenSys/rxas400/temp/as400histdocs/QOpenSys/rxas400/document");
        assertTrue(trash.stream().anyMatch(e -> "hello.txt".equals(e.name())));
        assertTrue(client.restoreIfsFile(trashPath));
        assertEquals("hello world", client.readIfsFile("/QOpenSys/rxas400/document/hello.txt"));
    }

    @Test
    void ifs_trashMissing_shouldReturnNull() {
        assertNull(client.trashIfsFile("/QOpenSys/rxas400/document/not-exist.txt"));
        assertFalse(client.restoreIfsFile("/QOpenSys/rxas400/temp/as400histdocs/x"));
    }

    @Test
    void ifs_mkdirAndWriteBlank_shouldGuard() {
        assertTrue(client.mkdirIfs("/QOpenSys/rxas400/deploy"));
        assertFalse(client.writeIfsFileBytes("", new byte[0]));
        assertFalse(client.mkdirIfs(""));
        assertNull(client.readIfsFileBytes(null));
    }

    // ==================== SPOOL 过滤 ====================

    @Test
    void listSpoolFiles_filters_shouldMatch() {
        assertEquals(3, client.listSpoolFiles(null, null, null).size());
        List<SpoolRow> byJob = client.listSpoolFiles("BATCH01", null, null);
        assertEquals(1, byJob.size());
        assertEquals("BATCH01", byJob.get(0).jobName());
        assertEquals(1, client.listSpoolFiles(null, null, "129305").size());
    }

    // ==================== 用户 profile ====================

    @Test
    void userProfile_ghost_shouldReturnNull() {
        assertNull(client.userProfile("GHOST0001"));
        assertNull(client.userProfile(""));
    }

    @Test
    void userProfile_admin_shouldMapGroup() {
        UserProfileRow admin = client.userProfile("admin");
        assertEquals("GRPADM", admin.groupProfile());
    }

    // ==================== 系统值 ====================

    @Test
    void changeSystemValue_okAndUnknown() {
        CommandResult ok = client.changeSystemValue("QMAXSIGN", "3");
        assertTrue(ok.success());
        assertEquals("3", client.listSystemValues().stream()
                .filter(v -> "QMAXSIGN".equals(v.name())).findFirst().orElseThrow().value());
        assertFalse(client.changeSystemValue("QUNKNOWN", "1").success());
        assertFalse(client.changeSystemValue("", "1").success());
    }

    // ==================== 消息 CRUD ====================

    @Test
    void message_crud_shouldWork() {
        assertTrue(client.addMessage(new MessageDescriptor("APP", "TESTMSG", "USR9001", "测试消息", "二级文本", 10)).success());
        assertFalse(client.addMessage(new MessageDescriptor("APP", "TESTMSG", "USR9001", "x", null, 1)).success());
        assertTrue(client.updateMessage(new MessageDescriptor("APP", "TESTMSG", "USR9001", "新文本", null, 20)).success());
        List<MessageRow> found = client.listMessages("APP", "TESTMSG", "新文本");
        assertEquals(1, found.size());
        assertEquals("USR9001", found.get(0).id());
        assertTrue(client.deleteMessage("APP", "TESTMSG", "USR9001").success());
        assertFalse(client.deleteMessage("APP", "TESTMSG", "USR9001").success());
        assertFalse(client.addMessage(new MessageDescriptor("APP", "", "X", "t", null, 1)).success());
    }

    // ==================== 子系统启停 ====================

    @Test
    void subsystem_startEnd_shouldFlip() {
        assertTrue(client.startSubsystem("QINTER").success());
        assertTrue(client.listSubsystems().stream()
                .anyMatch(s -> "QINTER".equals(s.name()) && "ACTIVE".equals(s.status())));
        assertTrue(client.endSubsystem("QINTER").success());
        assertTrue(client.listSubsystems().stream()
                .anyMatch(s -> "QINTER".equals(s.name()) && "INACTIVE".equals(s.status())));
        assertFalse(client.endSubsystem("").success());
    }

    // ==================== SLA / 依赖图（P1-7 缓存端点的数据源） ====================

    @Test
    void jobSlaExecutions_shouldReturnOkOrBreached() {
        List<JobSlaExecRow> rows = client.jobSlaExecutions();
        assertEquals(4, rows.size());
        assertTrue(rows.stream().allMatch(r -> "OK".equals(r.status()) || "BREACHED".equals(r.status())));
    }

    @Test
    void jobDependencies_shouldReturnNodesAndLinks() {
        GraphData g = client.jobDependencies();
        assertEquals(8, g.nodes().size());
        assertEquals(7, g.links().size());
    }
}
