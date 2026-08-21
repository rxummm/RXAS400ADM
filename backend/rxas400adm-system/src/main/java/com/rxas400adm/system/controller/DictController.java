package com.rxas400adm.system.controller;

import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.system.dto.DictItemDTO;
import com.rxas400adm.system.dto.DictTypeDTO;
import com.rxas400adm.system.service.IDictService;
import com.rxas400adm.system.vo.DictItemVO;
import com.rxas400adm.system.vo.DictTypeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 数据字典：类型 + 字典项两级管理（DICT_MANAGE），下拉引用接口登录即可读。
 */
@RestController
@RequestMapping("/api/v1/dicts")
@RequiredArgsConstructor
@Tag(name = "字典管理")
public class DictController {

    private final IDictService dictService;

    /** 全部字典类型（管理页） */
    @GetMapping("/types")
    public ApiResponse<List<DictTypeVO>> types() {
        return ApiResponse.success(dictService.listTypes().stream().map(DictTypeVO::from).toList());
    }

    @PostMapping("/types")
    @PreAuthorize("hasAuthority('DICT_MANAGE')")
    @OperateLog(module = "数据字典", operation = "新增字典类型")
    public ApiResponse<DictTypeVO> createType(@Valid @RequestBody DictTypeDTO type) {
        return ApiResponse.success(DictTypeVO.from(dictService.createType(type)));
    }

    @PutMapping("/types/{id}")
    @PreAuthorize("hasAuthority('DICT_MANAGE')")
    @OperateLog(module = "数据字典", operation = "修改字典类型")
    public ApiResponse<DictTypeVO> updateType(@PathVariable Long id, @Valid @RequestBody DictTypeDTO dto) {
        return ApiResponse.success(DictTypeVO.from(dictService.updateType(id, dto)));
    }

    @DeleteMapping("/types/{id}")
    @PreAuthorize("hasAuthority('DICT_MANAGE')")
    @OperateLog(module = "数据字典", operation = "删除字典类型")
    public ApiResponse<Void> deleteType(@PathVariable Long id) {
        dictService.deleteType(id);
        return ApiResponse.success(null);
    }

    /** 某类型的全部字典项（管理页） */
    @GetMapping("/items")
    public ApiResponse<List<DictItemVO>> items(@RequestParam String typeCode) {
        return ApiResponse.success(dictService.listItems(typeCode).stream().map(DictItemVO::from).toList());
    }

    /** 某类型启用中的字典项（el-select 下拉引用） */
    @GetMapping("/items/enabled")
    public ApiResponse<List<DictItemVO>> enabledItems(@RequestParam String typeCode) {
        return ApiResponse.success(dictService.enabledItems(typeCode).stream().map(DictItemVO::from).toList());
    }

    @PostMapping("/items")
    @PreAuthorize("hasAuthority('DICT_MANAGE')")
    @OperateLog(module = "数据字典", operation = "新增字典项")
    public ApiResponse<DictItemVO> createItem(@Valid @RequestBody DictItemDTO item) {
        return ApiResponse.success(DictItemVO.from(dictService.createItem(item)));
    }

    @PutMapping("/items/{id}")
    @PreAuthorize("hasAuthority('DICT_MANAGE')")
    @OperateLog(module = "数据字典", operation = "修改字典项")
    public ApiResponse<DictItemVO> updateItem(@PathVariable Long id, @Valid @RequestBody DictItemDTO dto) {
        return ApiResponse.success(DictItemVO.from(dictService.updateItem(id, dto)));
    }

    @DeleteMapping("/items/{id}")
    @PreAuthorize("hasAuthority('DICT_MANAGE')")
    @OperateLog(module = "数据字典", operation = "删除字典项")
    public ApiResponse<Void> deleteItem(@PathVariable Long id) {
        dictService.deleteItem(id);
        return ApiResponse.success(null);
    }
}