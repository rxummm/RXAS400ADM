# SQL 查询与业务数据

## 5.1 SQL 查询执行

`POST /api/v1/query/execute`

```mermaid
sequenceDiagram
    actor U as 用户
    participant QueryView as query/index.vue
    participant QueryCtrl as SqlQueryController
    participant QuerySvc as SqlQueryService
    participant AS400Client as AS400ClientProvider
    participant IBMi as IBM i (AS400)
    participant DB as 业务数据库

    U->>QueryView: 输入 SQL 语句
    QueryView->>QueryCtrl: POST /api/v1/query/execute<br/>🔒 QUERY_EXECUTE
    Note over QueryCtrl: @Valid QueryRequest(sql)
    QueryCtrl->>QuerySvc: execute(sql)

    Note over QuerySvc: SQL 安全校验
    QuerySvc->>QuerySvc: 只读 SELECT 校验<br/>拒绝 INSERT/UPDATE/DELETE/DROP/ALTER
    QuerySvc->>AS400Client: JTOpenSqlClient.executeQuery(sql)
    Note right of AS400Client: PreparedStatement<br/>DB2 for i
    AS400Client->>IBMi: 执行只读 SQL
    IBMi-->>AS400Client: ResultSet
    AS400Client-->>QuerySvc: List<Map<String,Object>>

    Note right of QuerySvc: ✏️ rx_sql_history<br/>保存查询历史
    QuerySvc-->>QueryCtrl: QueryResult
    QueryCtrl-->>QueryView: 查询结果表格

    Note over U: 查询历史
    U->>QueryView: 查看历史
    QueryView->>QueryCtrl: GET /api/v1/query/history?limit=20<br/>🔒 QUERY_EXECUTE
    Note right of QuerySvc: 📖 rx_sql_history
    QueryCtrl-->>QueryView: 历史记录
```

---

## 5.2 业务数据浏览 {#biz-data}

`GET /api/v1/business/*`

```mermaid
sequenceDiagram
    actor U as 用户
    participant BizView as business/index.vue
    participant BizCtrl as BusinessController
    participant BizSvc as BusinessService
    participant AS400Client as AS400ClientProvider
    participant IBMi as IBM i (AS400)

    Note over U: 库内文件清单
    U->>BizView: 选择库名
    BizView->>BizCtrl: GET /api/v1/business/tables?library=APP 🔒 QUERY_EXECUTE
    BizCtrl->>BizSvc: tables(library, keyword)
    BizSvc->>AS400Client: QSYS2.SYSTABLES
    AS400Client->>IBMi: 查询库内文件
    IBMi-->>BizView: 文件清单

    Note over U: 文件字段定义
    U->>BizView: 选择文件
    BizView->>BizCtrl: GET /api/v1/business/columns?library=APP&table=ORDER 🔒 QUERY_EXECUTE
    BizCtrl->>BizSvc: columns(library, table)
    BizSvc->>AS400Client: QSYS2.SYSCOLUMNS
    AS400Client->>IBMi: 查询字段定义
    IBMi-->>BizView: 字段定义（含长度/描述）

    Note over U: 业务数据分页浏览
    U->>BizView: 查看数据
    BizView->>BizCtrl: GET /api/v1/business/data?library=APP&table=ORDER&page=1&size=10&keyword=xxx 🔒 QUERY_EXECUTE
    BizCtrl->>BizSvc: data(library, table, keyword, page, size)
    BizSvc->>AS400Client: SELECT * FROM library.table<br/>WHERE 字符型字段 LIKE '%keyword%'<br/>PreparedStatement 参数化
    AS400Client->>IBMi: 执行查询
    IBMi-->>BizView: 分页数据
```

### 安全机制

- **只读校验**：后端拒绝 INSERT/UPDATE/DELETE/DROP/ALTER 等写入操作
- **参数化查询**：使用 PreparedStatement，防止 SQL 注入
- **权限控制**：`QUERY_EXECUTE` 权限码

### 涉及数据表

| 数据表/系统表 | 操作 | 说明 |
|--------------|------|------|
| 用户指定库表 | 🗄️ | 业务数据查询 |
| `QSYS2.SYSTABLES` | 🗄️ | 库内文件清单 |
| `QSYS2.SYSCOLUMNS` | 🗄️ | 字段定义 |
| `rx_sql_history` | 📖✏️ | 查询历史记录 |