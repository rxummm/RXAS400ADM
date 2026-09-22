package com.rxas400adm.mrp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.common.util.EntityUtil;
import com.rxas400adm.mrp.entity.BomLine;
import com.rxas400adm.mrp.entity.BomMaster;
import com.rxas400adm.mrp.mapper.BomLineMapper;
import com.rxas400adm.mrp.mapper.BomMasterMapper;
import com.rxas400adm.mrp.vo.BomLineVO;
import com.rxas400adm.mrp.vo.BomMasterVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BomService {

    private final BomMasterMapper bomMasterMapper;
    private final BomLineMapper bomLineMapper;

    public PageResult<BomMasterVO> pageQuery(String cono, String parentItem, String status,
                                             int current, int size) {
        LambdaQueryWrapper<BomMaster> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(cono)) wrapper.eq(BomMaster::getCono, cono);
        if (StringUtils.hasText(parentItem)) wrapper.like(BomMaster::getParentItem, parentItem);
        if (StringUtils.hasText(status)) wrapper.eq(BomMaster::getStatus, status);
        wrapper.orderByDesc(BomMaster::getCreatedTime);

        Page<BomMaster> page = bomMasterMapper.selectPage(new Page<>(current, size), wrapper);
        return new PageResult<>(page.getTotal(),
                page.getRecords().stream().map(BomMasterVO::from).toList());
    }

    public BomMasterVO getById(Long id) {
        BomMaster entity = EntityUtil.require(id, "BomMaster", bomMasterMapper::selectById);
        return BomMasterVO.from(entity);
    }

    public List<BomLineVO> getLines(Long bomId) {
        EntityUtil.require(bomId, "BomMaster", bomMasterMapper::selectById);
        return bomLineMapper.selectList(new LambdaQueryWrapper<BomLine>()
                .eq(BomLine::getBomId, bomId)
                .orderByAsc(BomLine::getLineNo))
                .stream().map(BomLineVO::from).toList();
    }

    /**
     * 递归展开BOM（最多展开maxLevels层）。
     */
    public List<BomLineVO> expandBom(String parentItem, int maxLevels) {
        return expandBomRecursive(parentItem, 0, maxLevels);
    }

    private List<BomLineVO> expandBomRecursive(String itemCode, int level, int maxLevels) {
        if (level >= maxLevels) return List.of();

        // 使用参数化查询防止 SQL 注入
        List<Long> bomIds = bomMasterMapper.selectList(new LambdaQueryWrapper<BomMaster>()
                .eq(BomMaster::getParentItem, itemCode)
                .eq(BomMaster::getStatus, "ACTIVE")
                .select(BomMaster::getId))
                .stream().map(BomMaster::getId).toList();

        if (bomIds.isEmpty()) return List.of();

        return bomLineMapper.selectList(new LambdaQueryWrapper<BomLine>()
                .in(BomLine::getBomId, bomIds)
                .orderByAsc(BomLine::getLineNo))
                .stream().map(BomLineVO::from).toList();
    }
}
