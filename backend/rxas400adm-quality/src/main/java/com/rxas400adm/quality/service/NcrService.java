package com.rxas400adm.quality.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.common.util.EntityUtil;
import com.rxas400adm.quality.dto.NcrCreateDTO;
import com.rxas400adm.quality.dto.NcrUpdateDTO;
import com.rxas400adm.quality.entity.Ncr;
import com.rxas400adm.quality.mapper.NcrMapper;
import com.rxas400adm.quality.vo.NcrVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NcrService {

    private final NcrMapper mapper;

    public PageResult<NcrVO> pageQuery(String itemCode, String status,
                                       LocalDate fromDate, LocalDate toDate,
                                       int current, int size) {
        LambdaQueryWrapper<Ncr> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(itemCode)) {
            wrapper.eq(Ncr::getItemCode, itemCode);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(Ncr::getStatus, status);
        }
        if (fromDate != null) {
            wrapper.ge(Ncr::getCreatedTime, fromDate.atStartOfDay());
        }
        if (toDate != null) {
            wrapper.le(Ncr::getCreatedTime, toDate.plusDays(1).atStartOfDay());
        }
        wrapper.orderByDesc(Ncr::getCreatedTime);

        Page<Ncr> page = mapper.selectPage(new Page<>(current, size), wrapper);
        return new PageResult<>(page.getTotal(), page.getRecords().stream()
                .map(NcrVO::from).toList());
    }

    public Ncr getByIdOrThrow(Long id) {
        return EntityUtil.require(id, "NCR", mapper::selectById);
    }

    public List<Ncr> listByItemCode(String itemCode) {
        return mapper.selectList(new LambdaQueryWrapper<Ncr>()
                .eq(Ncr::getItemCode, itemCode)
                .orderByDesc(Ncr::getCreatedTime));
    }

    public Ncr create(NcrCreateDTO dto) {
        Ncr ncr = new Ncr();
        ncr.setCono(dto.getCono());
        ncr.setInspectionId(dto.getInspectionId());
        ncr.setItemCode(dto.getItemCode());
        ncr.setItemDesc(dto.getItemDesc());
        ncr.setBatchNo(dto.getBatchNo());
        ncr.setQtyRejected(dto.getQtyRejected());
        ncr.setDefectType(dto.getDefectType());
        ncr.setDefectDescription(dto.getDefectDescription());
        ncr.setDisposition(dto.getDisposition());
        ncr.setDispositionDate(dto.getDispositionDate());
        ncr.setRootCause(dto.getRootCause());
        ncr.setCorrectiveAction(dto.getCorrectiveAction());
        ncr.setPreventiveAction(dto.getPreventiveAction());
        ncr.setAssignedTo(dto.getAssignedTo());
        ncr.setDueDate(dto.getDueDate());
        ncr.setStatus("OPEN");
        mapper.insert(ncr);
        return ncr;
    }

    public void updateStatus(Long id, NcrUpdateDTO dto) {
        Ncr ncr = getByIdOrThrow(id);
        ncr.setStatus(dto.getStatus());
        if ("CLOSED".equals(dto.getStatus())) {
            ncr.setCloseRemark(dto.getCloseRemark());
            ncr.setCloseDate(LocalDate.now());
        }
        if (StringUtils.hasText(dto.getDisposition())) {
            ncr.setDisposition(dto.getDisposition());
        }
        if (StringUtils.hasText(dto.getRootCause())) {
            ncr.setRootCause(dto.getRootCause());
        }
        if (StringUtils.hasText(dto.getCorrectiveAction())) {
            ncr.setCorrectiveAction(dto.getCorrectiveAction());
        }
        if (StringUtils.hasText(dto.getPreventiveAction())) {
            ncr.setPreventiveAction(dto.getPreventiveAction());
        }
        if (StringUtils.hasText(dto.getAssignedTo())) {
            ncr.setAssignedTo(dto.getAssignedTo());
        }
        mapper.updateById(ncr);
    }
}
