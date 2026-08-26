package com.rxas400adm.as400.service;

import com.rxas400adm.as400.dto.OpTemplateCreateDTO;
import com.rxas400adm.as400.dto.OpTemplateUpdateDTO;
import com.rxas400adm.as400.vo.OpTemplateVO;
import com.rxas400adm.common.response.PageResult;

/**
 * 操作模板/场景模式 Service。
 */
public interface IOpTemplateService {

    PageResult<OpTemplateVO> page(long current, long size, String keyword);

    OpTemplateVO create(OpTemplateCreateDTO dto, String username);

    OpTemplateVO update(Long id, OpTemplateUpdateDTO dto, String username);

    void delete(Long id);

    void execute(Long id, Long serverId);
}
