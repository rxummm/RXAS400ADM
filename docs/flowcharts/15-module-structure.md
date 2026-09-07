# 后端模块结构总览

> 自动生成方式：扫描 `backend/` 下所有 `.java` 文件，按包路径统计文件数。
> 此文档展示 RXAS400ADM 后端 7 个 Maven 模块的包结构与职责划分。

---

## rxas400adm-app（启动模块，82 文件）

```mermaid
graph TD
    subgraph app["rxas400adm-app — 启动模块 + 平台服务"]
        direction TB
        root["Rxas400admApplication.java<br/>主启动类"]

        subgraph config["config (24) — Spring 配置 + 平台服务"]
            cfgRoot["Config 层"]
            cfgSvc["service (2)<br/>PlatformTaskService<br/>AlertUpgradeService"]
            cfgVo["vo (4)<br/>TaskTriggerVO, TaskInfoVO<br/>HealthReportVO, CacheInfoVO"]
        end

        subgraph email["email (27) — 邮件子系统（完整 CRUD + 发送）"]
            emailCtrl["controller (4)<br/>EmailConfig/Group/Log/Send"]
            emailDto["dto (4)<br/>EmailSendDTO<br/>EmailRecipientDTO 等"]
            emailEnt["entity (4)<br/>EmailConfig, EmailLog<br/>EmailRecipient, EmailRecipientGroup"]
            emailMapper["mapper (4)<br/>MyBatis Plus Mappers"]
            emailSvc["service (6)<br/>EmailService, EmailLogService<br/>EmailGroupService + 接口"]
            emailVo["vo (4)<br/>EmailConfigVO, EmailGroupVO 等"]
        end

        subgraph inspection["inspection (4) — 系统巡检"]
            inspVo["vo (1)<br/>InspectionResultVO"]
        end

        subgraph report["report (26) — 报表引擎（调度 + 构建器）"]
            rptBuilder["builder (5) — 动态报表构建器"]
            rptBuilderDto["dto (1)<br/>ReportDefinitionDTO"]
            rptBuilderMapper["mapper (1)<br/>ReportDefinitionMapper"]
            rptBuilderVo["vo (2)<br/>ReportDefinitionVO<br/>DataSourceMeta"]
            rptDto["dto (1)<br/>ReportScheduleDTO"]
            rptMapper["mapper (2)<br/>ReportScheduleMapper<br/>ReportScheduleHistoryMapper"]
            rptVo["vo (2)<br/>ReportScheduleVO<br/>ReportScheduleHistoryVO"]
        end
    end

    root --> config
    root --> email
    root --> inspection
    root --> report
```

---

## rxas400adm-security（44 文件）

```mermaid
graph TD
    subgraph security["rxas400adm-security — 认证与安全"]
        direction TB

        subgraph secConfig["config (6) — 安全配置"]
            secCfgItems["SecurityConfig<br/>JwtProperties, CorsProperties<br/>RateLimitProperties<br/>ProxyProperties<br/>LoginSecurityProperties"]
        end

        subgraph secCtrl["controller (2)"]
            secCtrlItems["AuthController<br/>IpRuleController"]
        end

        subgraph secDto["dto (7)"]
            secDtoItems["LoginRequest, LoginResponse<br/>ChangePasswordDTO<br/>RefreshTokenDTO 等"]
        end

        subgraph secEntity["entity (3)"]
            secEntityItems["TokenBlacklist<br/>LoginAttempt, IpRule"]
        end

        subgraph secFilter["filter (2) — 请求过滤器"]
            secFilterItems["JwtAuthenticationFilter<br/>RateLimitFilter"]
        end

        subgraph secJwt["jwt (1)"]
            secJwtItems["JwtUtil"]
        end

        subgraph secMapper["mapper (3)"]
            secMapperItems["TokenBlacklistMapper<br/>LoginAttemptMapper<br/>IpRuleMapper"]
        end

        subgraph secSvc["service (14) — 核心安全服务"]
            secSvcItems["AuthService, PermissionService<br/>LoginAttemptService<br/>TokenBlacklistService<br/>IpRuleService<br/>As400LoginService 等"]
        end

        subgraph secVo["vo (6)"]
            secVoItems["TokenRefreshVO, ProfileVO<br/>MenuDataResponseVO 等"]
        end
    end
```

---

## rxas400adm-system（153 文件）

```mermaid
graph TD
    subgraph system["rxas400adm-system — 系统管理（用户/角色/菜单/审计）"]
        direction TB

        subgraph sysCtrl["controller (14)"]
            sysCtrlItems["SysUserController, SysRoleController<br/>SysMenuController, PermissionController<br/>DictController, ConfigController<br/>AuditLogController, NoticeController<br/>CalendarController, RegionController<br/>WebhookController, FavoriteController<br/>NotificationController, DashboardWidgetController"]
        end

        subgraph sysSvc["service (~30) — 业务逻辑"]
            sysSvcItems["SysUserService, RoleService<br/>MenuTreeService, MenuManageService<br/>PermissionManageService<br/>PermissionRequestService<br/>DictService, ConfigService<br/>AuditLogService, NoticeService<br/>CalendarEventService, RegionService<br/>NotificationService, WebhookService<br/>FavoriteService, I18nService 等"]
        end

        subgraph sysEntity["entity (~15)"]
            sysEntityItems["SysUser, SysRole, SysPermission<br/>SysMenu, DictType, DictItem<br/>AuditLog, Notice, CalendarEvent<br/>Region, Webhook, Favorite<br/>Notification, DashboardWidget<br/>PermissionRequest"]
        end

        subgraph sysMapper["mapper (5)"]
            sysMapperItems["SysMenuMapper, SysUserMenuMapper<br/>DictTypeMapper, DictItemMapper<br/>FavoriteMapper"]
        end

        subgraph sysDto["dto (~15)"]
            sysDtoItems["UserDTO, UserUpdateDTO<br/>RoleDTO, SysMenuDTO<br/>DictTypeDTO, DictItemDTO<br/>I18nEntryDTO 等"]
        end

        subgraph sysVo["vo (~12)"]
            sysVoItems["UserVO, SysRoleVO<br/>MenuVO, DictTypeVO<br/>I18nEntryVO 等"]
        end
    end
```

---

## rxas400adm-as400（474 文件 — 最大模块）

```mermaid
graph TD
    subgraph as400["rxas400adm-as400 — IBM i 连接 + BPCS 集成"]
        direction TB

        subgraph as400Root["核心连接层"]
            as400Provider["AS400ClientProvider / AS400ClientProviderImpl<br/>多服务器路由（X-AS400-Server 头）"]
            jtOpen["JTOpenAS400Client / JTOpenCommandClient<br/>JTOpenConnectionState"]
            mock["MockAS400Client（mock 模式）"]
        end

        subgraph as400Ctrl["controller (61) — REST API"]
            as400CtrlItems["As400Controller, JobController<br/>SqlQueryController, DocController<br/>IfsController, ObjectController<br/>BpcsOrderController, BpcsItemController<br/>BpcsCustomerController 等 61 个"]
        end

        subgraph as400Svc["service (112) — 业务逻辑"]
            as400SvcItems["JobService, DocService<br/>BpcsOrderServiceImpl<br/>BpcsItemServiceImpl<br/>CommandScriptService<br/>SqlQueryService 等 112 个"]
        end

        subgraph as400Mapper["mapper (~15)"]
            as400MapperItems["JobSlaMapper, OpTemplateMapper<br/>OrderChangeMapper, ShipmentMapper<br/>FreightCostMapper 等"]
        end

        subgraph as400Dto["dto (~30)"]
            as400DtoItems["BpcsOrderQueryDTO<br/>BpcsWabpConfigDTO<br/>BpcsRcmxConfigDTO 等"]
        end

        subgraph as400Vo["vo (~40)"]
            as400VoItems["BpcsOrderHeaderVO<br/>BpcsWabpConfigVO<br/>BpcsRcmxAssignmentVO 等"]
        end
    end
```

---

## rxas400adm-monitor（39 文件）

```mermaid
graph TD
    subgraph monitor["rxas400adm-monitor — 监控采集 + 告警"]
        direction TB

        subgraph monCtrl["controller (2)"]
            monCtrlItems["MonitorController<br/>AlertRuleController"]
        end

        subgraph monSvc["service (~10)"]
            monSvcItems["MetricService, MonitorService<br/>AlertRuleService, AlertEventService<br/>BaselineService, CapacityService<br/>CollectorScheduler"]
        end

        subgraph monCollector["collector (4) — 指标采集器"]
            monCollectorItems["DiskCollector, CpuCollector<br/>MemoryCollector, IfsCollector"]
        end

        subgraph monAlert["alert (2) — 告警引擎"]
            monAlertItems["AlertEngine, AlertRule"]
        end
    end
```

---

## rxas400adm-common（26 文件）

```mermaid
graph TD
    subgraph common["rxas400adm-common — 公共基础设施"]
        direction TB

        subgraph commonException["exception (3)"]
            commonExceptionItems["BusinessException<br/>ErrorCode（错误码枚举）<br/>GlobalExceptionHandler"]
        end

        subgraph commonConfig["config (3)"]
            commonConfigItems["ProfileResolver<br/>CorsProperties<br/>JacksonConfig"]
        end

        subgraph commonSecurity["security (3)"]
            commonSecurityItems["PasswordPolicy<br/>DangerousClCommandValidator<br/>SsrfGuard"]
        end

        subgraph commonUtil["util (4)"]
            commonUtilItems["EntityUtil, SecurityUtils<br/>PageConstants<br/>As400Identifiers"]
        end

        subgraph commonNotify["notify (2)"]
            commonNotifyItems["WebhookNotifier<br/>WebhookPayload"]
        end

        subgraph commonResponse["response / dto / constants"]
            commonOtherItems["ApiResponse, PageResult<br/>OperationLogAspect<br/>Constants"]
        end
    end
```

---

## rxas400adm-source（3 文件）

```mermaid
graph TD
    subgraph source["rxas400adm-source — 源码管理（桩模块）"]
        direction TB
        srcCtrl["controller (1)<br/>SourceController"]
        srcSvc["service (1)<br/>SourceService"]
        srcVo["vo (1)<br/>SourceFileVO"]
    end
```

---

## 模块依赖关系

```mermaid
graph TD
    app["rxas400adm-app<br/>(启动模块)"]
    as400["rxas400adm-as400<br/>(IBM i 集成)"]
    system["rxas400adm-system<br/>(系统管理)"]
    monitor["rxas400adm-monitor<br/>(监控告警)"]
    source["rxas400adm-source<br/>(源码管理)"]
    security["rxas400adm-security<br/>(认证安全)"]
    common["rxas400adm-common<br/>(公共基础)"]

    app --> as400
    app --> system
    app --> monitor
    app --> source
    app --> security
    app --> common
    as400 --> common
    system --> common
    monitor --> common
    security --> common
    source --> common

    style app fill:#e1f5fe
    style common fill:#fff9c4
    style security fill:#fce4ec
    style as400 fill:#e8f5e9
    style system fill:#f3e5f5
    style monitor fill:#fff3e0
    style source fill:#e0f2f1
```

---

## 分层规范（Controller → Service → Mapper）

```mermaid
graph LR
    subgraph Controller["Controller 层"]
        ctrlRules["✅ @RestController + @RequestMapping<br/>✅ @PreAuthorize 权限码<br/>✅ @OperateLog 审计注解<br/>❌ 禁止注入 Mapper<br/>❌ 禁止 new QueryWrapper"]
    end

    subgraph Service["Service 层"]
        svcRules["✅ @RequiredArgsConstructor 构造器注入<br/>✅ 方法参数用 DTO<br/>✅ 返回 ApiResponse&lt;VO&gt;<br/>❌ 禁止 @Autowired 字段注入<br/>❌ 禁止 @Transactional"]
    end

    subgraph Mapper["Mapper 层"]
        mapperRules["✅ extends BaseMapper&lt;Entity&gt;<br/>✅ 包名以 .mapper 结尾<br/>✅ 由 @MapperScan 自动扫描<br/>⚠️ @Mapper 注解冗余（已清理）"]
    end

    Controller -->|"调用接口"| Service
    Service -->|"继承 BaseMapper"| Mapper

    style Controller fill:#e3f2fd
    style Service fill:#e8f5e9
    style Mapper fill:#fff3e0
```
