package com.rxas400adm.system.controller;
import com.rxas400adm.common.util.SecurityUtils;

import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.system.service.IFavoriteService;
import com.rxas400adm.system.vo.FavoriteVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rxas400adm.system.dto.FavoriteToggleDTO;
import com.rxas400adm.system.vo.FavoriteToggleVO;

import jakarta.validation.Valid;
import java.util.List;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 快捷收藏：任意登录用户可收藏/取消收藏（顶栏星标 + 侧边栏收藏列表）。
 */
@RestController
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "快捷收藏")
public class FavoriteController {

    private final IFavoriteService favoriteService;

    @GetMapping("/mine")
    public ApiResponse<List<FavoriteVO>> mine() {
        return ApiResponse.success(favoriteService.mine(SecurityUtils.currentUsername()));
    }

    @GetMapping("/check")
    public ApiResponse<Boolean> check(@RequestParam String path) {
        return ApiResponse.success(favoriteService.isFavorited(SecurityUtils.currentUsername(), path));
    }

    @PostMapping("/toggle")
    @OperateLog(module = "快捷收藏", operation = "切换收藏")
    public ApiResponse<FavoriteToggleVO> toggle(@Valid @RequestBody FavoriteToggleDTO dto) {
        return ApiResponse.success(favoriteService.toggle(
                SecurityUtils.currentUsername(), dto.getTitle(), dto.getPath(), dto.getIcon()));
    }

    @DeleteMapping
    @OperateLog(module = "快捷收藏", operation = "取消收藏")
    public ApiResponse<Void> remove(@RequestParam String path) {
        favoriteService.remove(SecurityUtils.currentUsername(), path);
        return ApiResponse.success(null);
    }
}
