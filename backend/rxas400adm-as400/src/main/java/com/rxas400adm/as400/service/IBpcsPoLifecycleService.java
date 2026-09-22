package com.rxas400adm.as400.service;

import com.rxas400adm.as400.vo.BpcsPoLifecycleVO;
import com.rxas400adm.common.response.PageResult;

/**
 * ⑤ PO 全生命周期接口。
 */
public interface IBpcsPoLifecycleService {
    PageResult<BpcsPoLifecycleVO> listPoLifecycle(String cono, int current, int size);
}
