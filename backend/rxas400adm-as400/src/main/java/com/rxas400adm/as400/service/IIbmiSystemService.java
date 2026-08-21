package com.rxas400adm.as400.service;

import com.rxas400adm.as400.CommandResult;
import com.rxas400adm.as400.dto.IbmiSystemDTO;
import com.rxas400adm.as400.entity.IbmiSystem;
import com.rxas400adm.as400.vo.EnabledServerVO;
import com.rxas400adm.as400.vo.IbmiSystemVO;

import java.util.List;

/**
 * IBM i 服务器管理：服务器 CRUD、连接测试与 CL 命令执行。
 * 数据源按服务器 ID 路由（AS400ClientProvider.forServer）。
 */
public interface IIbmiSystemService {

    List<IbmiSystem> list();

    List<IbmiSystemVO> listVO();

    List<EnabledServerVO> listEnabled();

    IbmiSystem get(Long id);

    IbmiSystem create(IbmiSystemDTO system);

    IbmiSystem update(Long id, IbmiSystemDTO system);

    void delete(Long id);

    CommandResult testConnection(Long id);

    CommandResult executeCommand(Long id, String command);
}
