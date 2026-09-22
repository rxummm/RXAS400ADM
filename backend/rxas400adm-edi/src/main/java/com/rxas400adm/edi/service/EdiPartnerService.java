package com.rxas400adm.edi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.common.util.EntityUtil;
import com.rxas400adm.edi.entity.EdiPartner;
import com.rxas400adm.edi.mapper.EdiPartnerMapper;
import com.rxas400adm.edi.vo.EdiPartnerVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class EdiPartnerService {

    private final EdiPartnerMapper mapper;

    public PageResult<EdiPartnerVO> pageQuery(String partnerType, String status, int current, int size) {
        LambdaQueryWrapper<EdiPartner> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(partnerType)) wrapper.eq(EdiPartner::getPartnerType, partnerType);
        if (StringUtils.hasText(status)) wrapper.eq(EdiPartner::getStatus, status);
        wrapper.orderByDesc(EdiPartner::getCreatedTime);

        Page<EdiPartner> page = mapper.selectPage(new Page<>(current, size), wrapper);
        return new PageResult<>(page.getTotal(), page.getRecords().stream().map(EdiPartnerVO::from).toList());
    }

    public EdiPartnerVO getById(Long id) {
        EdiPartner entity = EntityUtil.require(id, "EdiPartner", mapper::selectById);
        return EdiPartnerVO.from(entity);
    }

    public EdiPartner getByCode(String partnerCode) {
        EdiPartner entity = mapper.selectOne(new LambdaQueryWrapper<EdiPartner>()
                .eq(EdiPartner::getPartnerCode, partnerCode));
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return entity;
    }
}
