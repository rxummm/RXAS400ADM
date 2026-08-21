package com.rxas400adm.system.controller;

import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.system.entity.Favorite;
import com.rxas400adm.system.service.IFavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    public ApiResponse<List<Favorite>> mine() {
        return ApiResponse.success(favoriteService.mine(currentUsername()));
    }

    @GetMapping("/check")
    public ApiResponse<Boolean> check(@RequestParam String path) {
        return ApiResponse.success(favoriteService.isFavorited(currentUsername(), path));
    }

    @PostMapping("/toggle")
    @OperateLog(module = "快捷收藏", operation = "切换收藏")
    public ApiResponse<FavoriteToggleVO> toggle(@Valid @RequestBody FavoriteToggleDTO dto) {
        return ApiResponse.success(favoriteService.toggle(
                currentUsername(), dto.getTitle(), dto.getPath(), dto.getIcon()));
    }

    @DeleteMapping
    @OperateLog(module = "快捷收藏", operation = "取消收藏")
    public ApiResponse<Void> remove(@RequestParam String path) {
        favoriteService.remove(currentUsername(), path);
        return ApiResponse.success(null);
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "anonymous" : authentication.getName();
    }
}
