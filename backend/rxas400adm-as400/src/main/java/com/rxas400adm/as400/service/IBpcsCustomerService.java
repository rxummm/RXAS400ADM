package com.rxas400adm.as400.service;

import com.rxas400adm.as400.dto.BpcsCustomerQueryDTO;
import com.rxas400adm.as400.vo.BpcsCustomerVO;
import com.rxas400adm.common.response.PageResult;

/**
 * 【AS400 业务增强·P2】客户档案查询（只读）。
 */
public interface IBpcsCustomerService {

    /** 按条件搜索客户列表（RCM 主档），支持分页 */
    PageResult<BpcsCustomerVO> search(BpcsCustomerQueryDTO query);

    /** 获取单个客户详情（含 Ship-To 列表） */
    BpcsCustomerVO getDetail(String cono, String cust);
}
