package com.rxas400adm.system.service;

import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.system.dto.SysDocDTO;
import com.rxas400adm.system.entity.SysDoc;
import com.rxas400adm.system.vo.SysDocVO;

/**
 * 知识库文档服务接口（纯 DB，无 IFS）。
 */
public interface ISysDocService {

    PageResult<SysDocVO> list(String keyword, String status, String category, long current, long size);

    SysDocVO detail(Long id);

    SysDocVO create(SysDocDTO dto, String operator);

    SysDocVO update(Long id, SysDocDTO dto, String operator);

    void delete(Long id);
}
