package com.rxas400adm.config.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.rxas400adm.monitor.alert.AlertEvent;
import com.rxas400adm.monitor.mapper.AlertEventMapper;
import com.rxas400adm.system.entity.Notification;
import com.rxas400adm.system.entity.SysUser;
import com.rxas400adm.system.mapper.SysUserMapper;
import com.rxas400adm.system.service.INotificationService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * E1 告警升级服务单测：覆盖开关关闭/无超时告警/无目标用户/正常定向通知 + 去重置位，
 * 以及修复回归点——逐用户定向（非全局广播）、标题落库、间隔配置为毫秒语义。
 */
@ExtendWith(MockitoExtension.class)
class AlertUpgradeServiceTest {

    @Mock
    private AlertEventMapper alertEventMapper;
    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private INotificationService notificationService;
    /** 【P3】notify-role 运行时来源 mock：get(key, default) 回传 default 等价未配置 rx_config */
    @Mock
    private com.rxas400adm.system.service.SysConfigService sysConfigService;

    /** 泛型捕获器：@Captor 注解处理嵌套泛型，避免 forClass 裸 Class 的 unchecked 警告 */
    @Captor
    private ArgumentCaptor<LambdaQueryWrapper<AlertEvent>> wrapperCaptor;

    /** P7：抢占式条件 UPDATE 的包装器捕获（@Captor 泛型安全） */
    @Captor
    private ArgumentCaptor<LambdaUpdateWrapper<AlertEvent>> updateWrapperCaptor;

    private AlertUpgradeService service;
    private AlertUpgradeProperties props;

    @org.junit.jupiter.api.BeforeAll
    static void initTableInfo() {
        // 纯 Mockito 环境无 MP 启动上下文：为断言 wrapper SQL 片段初始化 TableInfo 缓存
        MapperBuilderAssistant assistant =
                new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, AlertEvent.class);
    }

    @BeforeEach
    void setUp() {
        props = new AlertUpgradeProperties();
        props.setEnabled(true);
        props.setUpgradeAfterMinutes(30);
        props.setNotifyRole("ADMIN");
        // P3：未配置 rx_config 时回传 yml 缺省（get 第二参）
        org.mockito.Mockito.lenient()
                .when(sysConfigService.get(org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(inv -> inv.getArgument(1));
        service = new AlertUpgradeService(alertEventMapper, sysUserMapper, notificationService, props,
                sysConfigService);
    }

    private AlertEvent openAlert(long id) {
        AlertEvent alert = new AlertEvent();
        alert.setId(id);
        alert.setStatus("OPEN");
        alert.setLevel("CRITICAL");
        alert.setMessage("CPU 95%");
        alert.setCreatedTime(LocalDateTime.now().minusMinutes(60));
        alert.setUpgradeNotified(0);
        return alert;
    }

    private SysUser user(String name) {
        SysUser u = new SysUser();
        u.setUsername(name);
        return u;
    }

    /** 类型安全占位符：利用 any() 的目标类型推断消除裸 Class 字面量的 unchecked 转换警告 */
    private static LambdaQueryWrapper<AlertEvent> anyAlertWrapper() {
        return any();
    }

    /** C7：抢占式置位改用 LambdaUpdateWrapper，配套类型安全占位符 */
    private static LambdaUpdateWrapper<AlertEvent> anyAlertUpdateWrapper() {
        return any();
    }

    /** 构造「已被本节点抢占」的副本（upgrade_notified=1），供第二次查询桩返回 */
    private static AlertEvent claimedCopy(AlertEvent source) {
        AlertEvent claimed = new AlertEvent();
        claimed.setId(source.getId());
        claimed.setLevel(source.getLevel());
        claimed.setMessage(source.getMessage());
        claimed.setCreatedTime(source.getCreatedTime());
        claimed.setStatus(source.getStatus());
        claimed.setUpgradeNotified(1);
        return claimed;
    }

    @Test
    @DisplayName("开关关闭 → 直接返回，不查库不发通知")
    void disabled_shouldSkip() {
        props.setEnabled(false);
        service.checkAndUpgradeAlerts();
        verify(alertEventMapper, never()).selectList(any());
        verify(notificationService, never()).send(any(), any(), any(), any());
    }

    @Test
    @DisplayName("无超时未处理告警 → 不发通知")
    void noAlerts_shouldSkip() {
        when(alertEventMapper.selectList(anyAlertWrapper())).thenReturn(List.of());
        service.checkAndUpgradeAlerts();
        verify(notificationService, never()).send(any(), any(), any(), any());
    }

    @Test
    @DisplayName("角色下无用户 → 不发通知（已抢占的标记保留）")
    void noUsers_shouldSkipAndNotMark() {
        AlertEvent open = openAlert(1L);
        // C7 新流程：第一次查候选 → UPDATE 抢占 → 第二次查已抢占行 → 查用户
        when(alertEventMapper.selectList(anyAlertWrapper()))
                .thenReturn(List.of(open))
                .thenReturn(List.of(claimedCopy(open)));
        when(sysUserMapper.selectUsersByRoleCode("ADMIN")).thenReturn(List.of());
        service.checkAndUpgradeAlerts();
        verify(notificationService, never()).send(any(), any(), any(), any());
        // 抢占走批量条件 UPDATE（不再逐条 updateById）
        verify(alertEventMapper).update(isNull(), anyAlertUpdateWrapper());
        verify(alertEventMapper, never()).updateById(any(AlertEvent.class));
    }

    @Test
    @DisplayName("正常路径 → 每个目标用户各收一条定向通知（含标题），抢占式批量置位")
    void shouldSendTargetedNotificationPerUserAndMark() {
        AlertEvent a1 = openAlert(1L);
        AlertEvent a2 = openAlert(2L);
        // C7 新流程：第一次返回候选，UPDATE 抢占后第二次返回已置位的两行
        when(alertEventMapper.selectList(anyAlertWrapper()))
                .thenReturn(List.of(a1, a2))
                .thenReturn(List.of(claimedCopy(a1), claimedCopy(a2)));
        when(sysUserMapper.selectUsersByRoleCode("ADMIN")).thenReturn(List.of(user("alice"), user("bob")));
        when(notificationService.send(any(), any(), any(), any())).thenReturn(new Notification());

        service.checkAndUpgradeAlerts();

        // 定向：2 用户 × 1 次 = 2 条（旧缺陷实现是广播 N 遍发给全量用户）
        ArgumentCaptor<String> userCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService, times(2)).send(
                userCaptor.capture(), eq(AlertUpgradeService.NOTIFY_TYPE), any(), any());
        assertEquals(List.of("alice", "bob"), userCaptor.getAllValues());

        // 标题真实使用并落库
        verify(notificationService).send(eq("alice"), eq(AlertUpgradeService.NOTIFY_TYPE),
                eq("告警升级通知"), any());

        // P7：抢占为单条条件 UPDATE（WHERE id IN (...) AND upgrade_notified=0 SET ...=1）
        verify(alertEventMapper).update(isNull(), updateWrapperCaptor.capture());
        String setSql = updateWrapperCaptor.getValue().getSqlSet();
        assertEquals(true, setSql != null && setSql.contains("upgrade_notified"));
        verify(alertEventMapper, never()).updateById(any(AlertEvent.class));
    }

    @Test
    @DisplayName("查询条件必须含 upgrade_notified=0 去重过滤")
    void queryShouldFilterNotified() {
        lenient().when(alertEventMapper.selectList(anyAlertWrapper())).thenReturn(List.of());
        service.checkAndUpgradeAlerts();
        verify(alertEventMapper).selectList(wrapperCaptor.capture());
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        assertEquals(true, sqlSegment != null && sqlSegment.contains("upgrade_notified"));
    }
}
