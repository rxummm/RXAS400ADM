package com.rxas400adm.security;

import com.rxas400adm.common.util.ClientIpResolver;
import com.rxas400adm.security.config.CorsProperties;
import com.rxas400adm.security.config.JwtProperties;
import com.rxas400adm.security.config.LoginSecurityProperties;
import com.rxas400adm.security.config.ProxyProperties;
import com.rxas400adm.security.config.RateLimitProperties;
import jakarta.annotation.PostConstruct;
import org.mockito.stubbing.Answer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import com.rxas400adm.system.service.SysConfigService;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

/**
 * @WebMvcTest 切片配置锚点：替代启动类 {@code Rxas400admApplication} 的
 * {@code @MapperScan}，保证切片上下文不会创建真实 MyBatis Mapper Bean。
 * 保持与应用一致的 {@code @SpringBootApplication} 扫描范围；TypeExcludeFilter 会限制，
 * 使 {@code @WebMvcTest(controllers=...)} 只装配被注解指定的控制器——
 * 因此 R7 新增的 5 个 @ConfigurationProperties 需在此显式导入供过滤器/控制器注入。
 */
@SpringBootApplication(scanBasePackages = "com.rxas400adm")
@Import({
        JwtProperties.class,
        LoginSecurityProperties.class,
        RateLimitProperties.class,
        ProxyProperties.class,
        CorsProperties.class,
        ClientIpResolver.class,
})
public class TestSecuritySliceConfig {

    /**
     * 【第六章·P2】RateLimitFilter 运行时阈值改读 rx_config 后新增 SysConfigService 依赖——
     * 切片上下文以 Mock 提供（限流逻辑本身不经此切片验证），避免拖入真实 Mapper。
     */
    @MockBean
    private SysConfigService sysConfigService;

    /** 默认返回 defaultValue，避免 Mockito 默认 null → NPE（§14.3） */
    @PostConstruct
    void initMockDefaults() {
        Answer<String> returnDefault = invocation -> invocation.getArgument(1);
        lenient().when(sysConfigService.get(anyString(), anyString())).thenAnswer(returnDefault);
    }
}
