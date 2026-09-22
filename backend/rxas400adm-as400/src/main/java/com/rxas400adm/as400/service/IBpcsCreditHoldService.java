package com.rxas400adm.as400.service;

import com.rxas400adm.as400.vo.BpcsCreditHoldVO;
import com.rxas400adm.common.response.PageResult;

/**
 * ⑪ 信用 Hold 管理接口。
 */
public interface IBpcsCreditHoldService {
    PageResult<BpcsCreditHoldVO> listHoldOrders(String cono, int current, int size);
}
