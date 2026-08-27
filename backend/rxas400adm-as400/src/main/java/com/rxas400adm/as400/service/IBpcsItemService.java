package com.rxas400adm.as400.service;

import com.rxas400adm.as400.dto.BpcsItemQueryDTO;
import com.rxas400adm.as400.vo.BpcsItemVO;
import com.rxas400adm.common.response.PageResult;

/**
 * 【AS400 业务增强·P2】物料主档查询（只读，多维度 Tab）。
 */
public interface IBpcsItemService {
    /** 按条件搜索物料列表（基础信息），支持分页 */
    PageResult<BpcsItemVO> search(BpcsItemQueryDTO query);

    /** 获取单个物料详情（含库存/采购/销售全维度） */
    BpcsItemVO getDetail(String item);
}
