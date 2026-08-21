package com.rxas400adm.config;

import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.config.vo.CacheInfoVO;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 缓存管理（Spring Cache）：查看 CacheManager 下各缓存名称与条目数，
 * 支持按缓存名 / 全量清空。未启用 Spring Cache（无 CacheManager Bean）时返回空列表。
 * 权限码：SYS_CACHE_MANAGE。
 */
@RestController
@RequestMapping("/api/v1/caches")
@Tag(name = "缓存管理")
public class CacheController {

    private final ObjectProvider<CacheManager> cacheManagerProvider;

    public CacheController(ObjectProvider<CacheManager> cacheManagerProvider) {
        this.cacheManagerProvider = cacheManagerProvider;
    }

    private CacheManager cacheManager() {
        return cacheManagerProvider.getIfAvailable();
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SYS_CACHE_MANAGE')")
    public ApiResponse<List<CacheInfoVO>> caches() {
        CacheManager cacheManager = cacheManager();
        List<CacheInfoVO> result = new ArrayList<>();
        if (cacheManager == null) {
            return ApiResponse.success(result);
        }
        for (String name : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(name);
            Long size = null;
            if (cache != null && cache.getNativeCache() instanceof com.github.benmanes.caffeine.cache.Cache<?, ?> caffeine) {
                size = (long) caffeine.estimatedSize();
            }
            result.add(new CacheInfoVO(name, size));
        }
        return ApiResponse.success(result);
    }

    /** 清空指定缓存 */
    @DeleteMapping("/{name}")
    @PreAuthorize("hasAuthority('SYS_CACHE_MANAGE')")
    @OperateLog(module = "缓存管理", operation = "清空缓存")
    public ApiResponse<Void> clear(@PathVariable String name) {
        CacheManager cacheManager = cacheManager();
        Cache cache = cacheManager == null ? null : cacheManager.getCache(name);
        if (cache != null) {
            cache.clear();
        }
        return ApiResponse.success(null);
    }

    /** 清空全部缓存 */
    @DeleteMapping
    @PreAuthorize("hasAuthority('SYS_CACHE_MANAGE')")
    @OperateLog(module = "缓存管理", operation = "清空全部缓存")
    public ApiResponse<Void> clearAll() {
        CacheManager cacheManager = cacheManager();
        if (cacheManager != null) {
            for (String name : cacheManager.getCacheNames()) {
                Cache cache = cacheManager.getCache(name);
                if (cache != null) {
                    cache.clear();
                }
            }
        }
        return ApiResponse.success(null);
    }
}