package com.rxas400adm.as400.service;

import com.rxas400adm.as400.vo.BpcsCreditHoldVO;

import java.util.List;

/**
 * ⑪ 信用 Hold 管理接口。
 */
public interface IBpcsCreditHoldService {
    List<BpcsCreditHoldVO> listHoldOrders(String cono, int limit);
}
