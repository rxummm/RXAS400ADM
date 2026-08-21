package com.rxas400adm.as400.service;

import com.rxas400adm.as400.CommandResult;
import com.rxas400adm.as400.model.SubsystemRow;

import java.util.List;

/**
 * 系统服务管理：子系统状态列表（QSYS2.SUBSYSTEM_INFO）与启停操作。
 * 数据源按 X-AS400-Server 路由。
 */
public interface ISubsystemService {

    List<SubsystemRow> list();

    CommandResult start(String name);

    CommandResult end(String name);
}
