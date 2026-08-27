package com.rxas400adm.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.common.util.EntityUtil;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.system.dto.RegionDTO;
import com.rxas400adm.system.entity.Region;
import com.rxas400adm.system.mapper.RegionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 行政区划（rx_region）：懒加载子级、分页/搜索、CRUD。
 * 参照旧项目 ChinaRegionService。
 */
@Service
@RequiredArgsConstructor
public class RegionService implements IRegionService {

    private final RegionMapper regionMapper;

    /** 下级行政区划（parentCode 空=省；配合前端懒加载树） */
    public List<Region> children(String parentCode) {
        return regionMapper.selectList(new LambdaQueryWrapper<Region>()
                .eq(Region::getParentCode, parentCode == null ? "" : parentCode)
                .eq(Region::getStatus, 1)
                .orderByAsc(Region::getCode));
    }

    /** 分页查询（关键字/层级/上级代码过滤） */
    public PageResult<Region> page(int current, int size, String keyword, Integer level, String parentCode) {
        LambdaQueryWrapper<Region> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(q -> q.like(Region::getName, kw).or().like(Region::getCode, kw));
        }
        if (level != null) {
            wrapper.eq(Region::getLevel, level);
        }
        if (StringUtils.hasText(parentCode)) {
            wrapper.eq(Region::getParentCode, parentCode.trim());
        }
        wrapper.orderByAsc(Region::getCode);
        Page<Region> page = regionMapper.selectPage(
                new Page<>(PageConstants.clampNum(current), PageConstants.clampSize(size)), wrapper);
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    /** 关键字搜索（供级联选择器/联想用），最多 50 条 */
    public List<Region> search(String keyword, Integer level) {
        LambdaQueryWrapper<Region> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(q -> q.like(Region::getName, kw).or().like(Region::getCode, kw));
        }
        if (level != null) {
            wrapper.eq(Region::getLevel, level);
        }
        wrapper.eq(Region::getStatus, 1).orderByAsc(Region::getCode).last(PageConstants.limitClause(50));
        return regionMapper.selectList(wrapper);
    }

    
    public Region create(RegionDTO dto) {
        Region region = new Region();
        region.setCode(dto.getCode());
        region.setName(dto.getName());
        region.setLevel(dto.getLevel());
        region.setParentCode(dto.getParentCode());
        region.setPinyin(dto.getPinyin());
        region.setAbbreviation(dto.getAbbreviation());
        region.setLongitude(dto.getLongitude());
        region.setLatitude(dto.getLatitude());
        region.setSort(dto.getSort());
        region.setStatus(dto.getStatus());
        if (!StringUtils.hasText(region.getCode()) || !StringUtils.hasText(region.getName())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "行政区划代码与名称必填");
        }
        long exists = regionMapper.selectCount(new LambdaQueryWrapper<Region>()
                .eq(Region::getCode, region.getCode().trim()));
        if (exists > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "行政区划代码已存在: " + region.getCode());
        }
        region.setId(null);
        region.setCode(region.getCode().trim());
        region.setParentCode(region.getParentCode() == null ? "" : region.getParentCode().trim());
        region.setLevel(region.getLevel() == null ? 1 : region.getLevel());
        region.setStatus(region.getStatus() == null ? 1 : region.getStatus());
        region.setCreatedTime(LocalDateTime.now());
        region.setUpdatedTime(LocalDateTime.now());
        regionMapper.insert(region);
        return region;
    }

    
    public Region update(Long id, RegionDTO dto) {
        Region region = EntityUtil.require(id, "行政区划", regionMapper::selectById);
        if (StringUtils.hasText(dto.getCode())) {
            region.setCode(dto.getCode().trim());
        }
        if (StringUtils.hasText(dto.getName())) {
            region.setName(dto.getName());
        }
        if (dto.getLevel() != null) {
            region.setLevel(dto.getLevel());
        }
        if (dto.getParentCode() != null) {
            region.setParentCode(dto.getParentCode().trim());
        }
        if (dto.getPinyin() != null) region.setPinyin(dto.getPinyin());
        if (dto.getAbbreviation() != null) region.setAbbreviation(dto.getAbbreviation());
        if (dto.getLongitude() != null) region.setLongitude(dto.getLongitude());
        if (dto.getLatitude() != null) region.setLatitude(dto.getLatitude());
        if (dto.getSort() != null) region.setSort(dto.getSort());
        if (dto.getStatus() != null) region.setStatus(dto.getStatus());
        region.setUpdatedTime(LocalDateTime.now());
        regionMapper.updateById(region);
        return region;
    }

    
    public void delete(Long id) {
        Region region = EntityUtil.require(id, "行政区划", regionMapper::selectById);
        Long children = regionMapper.selectCount(new LambdaQueryWrapper<Region>()
                .eq(Region::getParentCode, region.getCode()));
        if (children > 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "该行政区划下存在下级数据，无法删除");
        }
        regionMapper.deleteById(id);
    }


}