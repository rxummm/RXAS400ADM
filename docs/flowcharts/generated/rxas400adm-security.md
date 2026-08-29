# rxas400adm-security 模块（自动生成）

> ⚠️ 此文件由 `scripts/generate-flow-diagrams.cjs` 自动生成，请勿手动编辑。

> 生成时间：2026-08-28T15:50:32.336Z

## root

### POST /logout

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as AuthController

    Note over F: POST /logout
    Note over P1: 🔒 USER_MANAGE
    F->>P1: POST /logout
```

### POST /login

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as AuthController

    Note over F: POST /login
    Note over P1: 🔒 USER_MANAGE
    F->>P1: POST /login
```

### POST /as400-login

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as AuthController

    Note over F: POST /as400-login
    Note over P1: 🔒 USER_MANAGE
    F->>P1: POST /as400-login
```

### GET /login-attempts

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as AuthController

    Note over F: GET /login-attempts
    Note over P1: 🔒 USER_MANAGE
    F->>P1: GET /login-attempts
```

### POST /change-password

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as AuthController

    Note over F: POST /change-password
    Note over P1: 🔒 USER_MANAGE
    F->>P1: POST /change-password
```

### POST /refresh

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as AuthController

    Note over F: POST /refresh
    Note over P1: 🔒 USER_MANAGE
    F->>P1: POST /refresh
```

### GET /profile

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as AuthController

    Note over F: GET /profile
    Note over P1: 🔒 USER_MANAGE
    F->>P1: GET /profile
```

### GET /menu

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as AuthController

    Note over F: GET /menu
    Note over P1: 🔒 USER_MANAGE
    F->>P1: GET /menu
```

### GET /page

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as IpRuleController

    Note over F: GET /page
    Note over P1: 🔒 SYS_IP_MANAGE
    F->>P1: GET /page
```

### PUT /{id}

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as IpRuleController

    Note over F: PUT /{id}
    Note over P1: 🔒 SYS_IP_MANAGE
    F->>P1: PUT /{id}
```

### DELETE /{id}

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as IpRuleController

    Note over F: DELETE /{id}
    Note over P1: 🔒 SYS_IP_MANAGE
    F->>P1: DELETE /{id}
```

## ips

### GET /login-attempts/ips

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as AuthController

    Note over F: GET /login-attempts/ips
    Note over P1: 🔒 USER_MANAGE
    F->>P1: GET /login-attempts/ips
```

## {username}

### DELETE /login-attempts/{username}

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as AuthController

    Note over F: DELETE /login-attempts/{username}
    Note over P1: 🔒 USER_MANAGE
    F->>P1: DELETE /login-attempts/{username}
```

## v1

### ALL /api/v1/auth

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as AuthController

    Note over F: ALL /api/v1/auth
    Note over P1: 🔒 USER_MANAGE
    F->>P1: ALL /api/v1/auth
```

### ALL /api/v1/ip-rules

```mermaid
sequenceDiagram
    participant F as 前端
    participant P1 as IpRuleController

    Note over F: ALL /api/v1/ip-rules
    Note over P1: 🔒 SYS_IP_MANAGE
    F->>P1: ALL /api/v1/ip-rules
```
