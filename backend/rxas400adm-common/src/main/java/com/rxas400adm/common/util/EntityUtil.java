package com.rxas400adm.common.util;

import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;

import java.util.function.Function;

/**
 * 实体查询辅助工具：消除各 Service 中重复的 require(id) 样板。
 *
 * <p>Usage: {@code EntityUtil.require(id, "Notice", noticeMapper::selectById)}
 * — throws {@code BusinessException(NOT_FOUND, "Notice not found: {id}")} when not found.
 */
public final class EntityUtil {

    private EntityUtil() {
    }

    /**
     * 按主键查询实体，不存在则抛 {@link BusinessException}。
     *
     * @param id         主键值
     * @param entityName Entity display name for error messages (e.g. "Notice", "Webhook")
     * @param lookup     Actual query function (typically mapper::selectById)
     * @param <T>        Entity type
     * @return The queried entity
     */
    public static <T> T require(Long id, String entityName, Function<Long, T> lookup) {
        T entity = lookup.apply(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, entityName + " not found: " + id);
        }
        return entity;
    }
}
