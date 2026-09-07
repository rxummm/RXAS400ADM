package com.rxas400adm.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rxas400adm.as400.entity.IbmiSystem;
import com.rxas400adm.as400.mapper.IbmiSystemMapper;
import com.rxas400adm.common.crypto.AesCryptoService;
import com.rxas400adm.monitor.alert.AlertRule;
import com.rxas400adm.monitor.mapper.AlertRuleMapper;
import com.rxas400adm.system.entity.SysRole;
import com.rxas400adm.system.entity.SysUser;
import com.rxas400adm.system.entity.SysUserRole;
import com.rxas400adm.system.mapper.SysRoleMapper;
import com.rxas400adm.system.mapper.SysUserMapper;
import com.rxas400adm.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 首次启动初始化演示数据（M1 收敛后职责精简，V48 再收敛）：
 * - 平台基础结构（权限码 / 角色 / 菜单树 / 角色授权）由 Flyway V38 统一幂等播种（单一数据源，
 *   不再与迁移文件双维护）。
 * - 默认系统参数（组角色映射 / Webhook）与文档模板已下沉 Flyway V48（任何环境都播种）；
 *   本类只负责 mock/dev profile 下的演示数据（admin 账号 / 示例 IBM i 实例 / 示例告警规则）。
 * - 非 mock/dev 的全新库拒绝注入演示数据并 fail-fast（P0-5，参照 StartupGuard 对默认密钥的处理）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final IbmiSystemMapper systemMapper;
    private final AlertRuleMapper alertRuleMapper;
    private final PasswordEncoder passwordEncoder;
    private final AesCryptoService aesCryptoService;
    private final Environment environment;

    @Override
    public void run(String... args) {
        if (userMapper.selectCount(null) > 0) {
            // 已有数据：权限码/菜单/角色授权由 V38 幂等覆盖，演示数据在全新库时才注入
            return;
        }
        // 全新数据库：演示数据（admin 账号、示例服务器、告警规则）仅在 mock/dev 注入（P0-5）
        boolean demoProfile = environment.acceptsProfiles(Profiles.of("mock", "dev"));
        if (!demoProfile) {
            // 参照 StartupGuard 对默认 JWT 密钥的处理：非 mock/dev 的全新库拒绝注入演示数据，fail-fast
            throw new IllegalStateException(
                    "Fresh database on non-mock/dev profile: to prevent demo data (admin/admin123, sample servers) entering production, "
                            + "initialize admin account and base data manually, or start with mock/dev profile.");
        }
        initAdmin();
        initIbmiSystems();
        initAlertRules();
        log.info("Demo data initialized: admin / admin123");
    }

    /** 演示管理员：admin / admin123；角色-权限、角色-菜单绑定已由 V38 种子提供（ADMIN=全量） */
    private void initAdmin() {
        SysUser admin = new SysUser();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setEmail("admin@rxas400.local");
        admin.setStatus("ACTIVE");
        admin.setCreatedTime(LocalDateTime.now());
        admin.setUpdatedTime(LocalDateTime.now());
        userMapper.insert(admin);

        SysRole adminRole = roleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, "ADMIN"));
        /* B12：V38 种子被破坏时 fail-fast 给出明确原因，替代裸 NPE */
        if (adminRole == null) {
            throw new IllegalStateException("ADMIN role missing: V38 seed data corrupted, check Flyway migrations");
        }
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(admin.getId());
        userRole.setRoleId(adminRole.getId());
        userRoleMapper.insert(userRole);
    }

    private void initIbmiSystems() {
        insertSystem("PROD400", "10.1.1.10", "PROD", "CRITICAL", "HA-GROUP-1");
        insertSystem("TEST400", "10.1.1.20", "TEST", "NORMAL", "HA-GROUP-1");
        insertSystem("DEV400", "10.1.1.30", "DEV", "LOW", null);
        insertSystem("DR400", "10.2.1.10", "DR", "CRITICAL", "HA-GROUP-1");
    }

    private void insertSystem(String name, String host, String env, String level, String haGroup) {
        IbmiSystem system = new IbmiSystem();
        system.setName(name);
        system.setHost(host);
        system.setPort(8470);
        system.setUsername("DEMOADM");
        system.setPasswordEncrypt(aesCryptoService.encrypt("demo-placeholder"));
        system.setEnvironment(env);
        system.setRegion("CN-EAST");
        system.setCriticalLevel(level);
        system.setHaGroup(haGroup);
        system.setEnabled(true);
        system.setDefaultServer("PROD400".equals(name));
        system.setSslEnabled(false);
        system.setCcsid(37);
        system.setDefaultLibraries("QSYS,QTEMP");
        system.setSortOrder(1);
        system.setStatus("ONLINE");
        system.setCreatedTime(LocalDateTime.now());
        systemMapper.insert(system);
    }

    private void initAlertRules() {
        insertRule("CPU", ">", 90.0, 300, "CRITICAL", "ALL", null, "cpuCritical");
        insertRule("CPU", ">", 80.0, 600, "WARNING", "ALL", null, "cpuWarning");
        insertRule("MEMORY", ">", 85.0, 300, "WARNING", "ALL", null, "memoryWarning");
    }

    private void insertRule(String metricName, String operator, double threshold,
                            int duration, String level, String channel, Long serverId,
                            String description) {
        AlertRule rule = new AlertRule();
        rule.setMetricName(metricName);
        rule.setOperator(operator);
        rule.setThreshold(threshold);
        rule.setDurationSeconds(duration);
        rule.setLevel(level);
        rule.setEnabled(true);
        rule.setChannel(channel);
        rule.setServerId(serverId);
        rule.setDescription(description);
        alertRuleMapper.insert(rule);
    }
}