# 请求拦截器链

## 9.1 前端 Axios 请求/响应拦截器

位于 `frontend/src/utils/request.ts`

```mermaid
sequenceDiagram
    participant Vue as Vue 组件
    participant Axios as Axios (request.ts)
    participant Pinia as Pinia userStore
    participant Backend as 后端

    Note over Vue: 发起请求
    Vue->>Axios: axios.get/post/put/delete(url, data)

    Note over Axios: 请求拦截器
    Axios->>Axios: NProgress.start()
    Axios->>Pinia: useUserStore().token
    Pinia-->>Axios: token
    alt token 存在
        Axios->>Axios: headers.Authorization = "Bearer " + token
    end
    Axios->>Pinia: useAs400ServerStore().selectedServerId
    alt 已选择服务器
        Axios->>Axios: headers["X-AS400-Server"] = serverId
    end
    Axios->>Axios: Content-Type: application/json
    Axios->>Backend: 发送请求

    Note over Axios: 响应拦截器
    Backend-->>Axios: HTTP 响应
    Axios->>Axios: NProgress.done()

    alt 响应 code === 200
        Axios-->>Vue: response.data
    else 响应 code === 401
        Axios->>Pinia: logout()
        Axios->>Vue: ElMessage.error("登录过期")
        Axios->>Vue: router.push('/login')
    else 响应 code === 403
        Axios->>Vue: ElMessage.error("权限不足")
    else 其他错误
        Axios->>Vue: ElMessage.error(message)
    end
```

---

## 9.2 后端 Security Filter 链

位于 `backend/.../security/`

```mermaid
sequenceDiagram
    participant Client as 浏览器
    participant CorsFilter as CorsFilter
    participant SecurityConfig as SecurityConfig
    participant JwtFilter as JwtAuthenticationFilter
    participant Controller as Controller
    participant Exception as 全局异常处理

    Client->>CorsFilter: HTTP Request
    Note over CorsFilter: CORS 白名单校验<br/>允许源 / 方法 / 头
    CorsFilter->>SecurityConfig: 通过 CORS

    Note over SecurityConfig: CSP / CSRF / Session 策略<br/>放行白名单: /api/v1/auth/login, /api/v1/auth/as400-login, /api/v1/health

    alt 白名单路径
        SecurityConfig->>Controller: 直接放行
    else 安全路径
        SecurityConfig->>JwtFilter: 进入 JWT 校验
        Note over JwtFilter: 从 Authorization 头提取 Bearer token<br/>解析 JWT → 校验签名 + 过期<br/>提取 username + permissions
        JwtFilter->>JwtFilter: 校验 token 是否在黑名单
        Note right of JwtFilter: 📖 rx_token_blacklist

        alt token 无效 / 已吊销 / 过期
            JwtFilter-->>Client: 401 Unauthorized
        else token 有效
            JwtFilter->>JwtFilter: 构建 Authentication 对象<br/>UsernamePasswordAuthenticationToken
            JwtFilter->>Controller: SecurityContext 设置认证信息
            Controller-->>Client: 业务响应
        end
    end

    Note over Exception: 全局异常处理
    alt 发生异常
        Controller->>Exception: throw
        Exception-->>Client: ApiResponse(code, message)
    end
```

### Filter 链顺序

```
HTTP Request
  → CorsFilter (CORS 白名单)
  → SecurityConfig (CSP / CSRF / Session / 白名单路径)
  → JwtAuthenticationFilter (JWT 校验 + 黑名单 + 权限注入)
  → Controller (业务处理)
  → 全局异常处理 (统一响应格式)
```