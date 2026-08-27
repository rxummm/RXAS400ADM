package com.rxas400adm.common.util;

import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;

import java.util.function.Function;

/**
 * 实体查询辅助工具：消除各 Service 中重复的 require(id) 样板。
 *
 * <p>用法：{@code EntityUtil.require(id, "公告", noticeMapper::selectById)}
 * —— 查询不到则抛出 {@code BusinessException(NOT_FOUND, "公告不存在: {id}")}。
 */
public final class EntityUtil {

    private EntityUtil() {
    }

    /**
     * 按主键查询实体，不存在则抛 {@link BusinessException}。
     *
     * @param id         主键值
     * @param entityName 实体中文名称（用于错误提示，如 "公告"、"Webhook"）
     * @param lookup     实际查询函数（通常为 mapper::selectById）
     * @param <T>        实体类型
     * @return 查询到的实体
     */
    public static <T> T require(Long id, String entityName, Function<Long, T> lookup) {
        T entity = lookup.apply(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, entityName + "不存在: " + id);
        }
        return entity;
    }
}
