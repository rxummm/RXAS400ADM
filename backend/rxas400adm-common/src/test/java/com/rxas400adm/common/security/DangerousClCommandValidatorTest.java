package com.rxas400adm.common.security;

import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 【第六章·P1】黑名单校验器运行时配置测试：
 * provider 三态（关闭/扩展命中/内置命中）+ B3 行内注释绕过回归。
 */
class DangerousClCommandValidatorTest {

    /** 测试用固定 provider */
    private static ClBlacklistConfigProvider provider(boolean enabled, List<String> extra) {
        return new ClBlacklistConfigProvider() {
            @Override
            public boolean enabled() {
                return enabled;
            }

            @Override
            public List<String> extraVerbs() {
                return extra;
            }
        };
    }

    @Test
    @DisplayName("开关关闭 → 任意命令放行")
    void disabled_shouldAllowAll() {
        DangerousClCommandValidator validator =
                new DangerousClCommandValidator(provider(false, List.of("CHGJOB")));
        assertDoesNotThrow(() -> validator.assertAllowed("CHGUSRPRF USER(X)"));
    }

    @Test
    @DisplayName("内置动词命中 → FORBIDDEN")
    void builtinVerbHit_shouldReject() {
        DangerousClCommandValidator validator =
                new DangerousClCommandValidator(provider(true, List.of()));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> validator.assertAllowed("DLTLIB LIB(TEMP)"));
        assertEquals(ErrorCode.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("扩展动词命中 → FORBIDDEN（rx_config 追加生效）")
    void extraVerbHit_shouldReject() {
        DangerousClCommandValidator validator =
                new DangerousClCommandValidator(provider(true, List.of("CHGJOB")));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> validator.assertAllowed("CHGJOB JOB(X)"));
        assertEquals(ErrorCode.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("B3 回归：行内注释后接高危命令仍被拦截")
    void inlineCommentBypass_shouldReject() {
        DangerousClCommandValidator validator =
                new DangerousClCommandValidator(provider(true, List.of()));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> validator.assertAllowed("/* 注释 */ CHGUSRPRF USRPRF(HACK)"));
        assertEquals(ErrorCode.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("非高危命令 → 放行")
    void normalCommand_shouldPass() {
        DangerousClCommandValidator validator =
                new DangerousClCommandValidator(provider(true, List.of()));
        assertDoesNotThrow(() -> validator.assertAllowed("DSPSYSVAL QDATETIME"));
    }
}
