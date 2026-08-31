package com.rxas400adm.as400.service;

import com.rxas400adm.as400.vo.BpcsBomLineVO;

import java.util.List;

/**
 * ① BOM 查询展开接口。
 */
public interface IBpcsBomService {

    /** 查找该物料用在哪些成品（反查） */
    List<BpcsBomLineVO> findParents(String cono, String component);

    /** 展开某个成品的 BOM（正查一层子件） */
    List<BpcsBomLineVO> expandChildren(String cono, String parent);
}
