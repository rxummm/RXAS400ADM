package com.rxas400adm.as400.service;

import com.rxas400adm.as400.vo.BpcsPoLifecycleVO;

import java.util.List;

/**
 * ⑤ PO 全生命周期接口。
 */
public interface IBpcsPoLifecycleService {
    List<BpcsPoLifecycleVO> listPoLifecycle(String cono, int limit);
}
