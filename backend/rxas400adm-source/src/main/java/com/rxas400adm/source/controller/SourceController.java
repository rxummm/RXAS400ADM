package com.rxas400adm.source.controller;

import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.source.service.ISourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/source")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SOURCE_VIEW')")
@Tag(name = "源码管理")
public class SourceController {

    private final ISourceService sourceService;

    @GetMapping("/libraries")
    public ApiResponse<List<String>> libraries() {
        return ApiResponse.success(sourceService.listLibraries());
    }

    @GetMapping("/libraries/{library}/files")
    public ApiResponse<List<String>> files(@PathVariable String library) {
        return ApiResponse.success(sourceService.listSourceFiles(library));
    }

    @GetMapping("/libraries/{library}/files/{file}/members")
    public ApiResponse<List<String>> members(@PathVariable String library, @PathVariable String file) {
        return ApiResponse.success(sourceService.listMembers(library, file));
    }

    @GetMapping("/libraries/{library}/files/{file}/members/{member}")
    public ApiResponse<Map<String, String>> member(@PathVariable String library,
                                                   @PathVariable String file,
                                                   @PathVariable String member) {
        return ApiResponse.success(sourceService.readMember(library, file, member));
    }
}