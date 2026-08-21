package com.rxas400adm.as400;

import com.rxas400adm.as400.model.SubsystemRow;

import java.util.List;

/**
 * 子系统域：状态列表（QSYS2.SUBSYSTEM_INFO）与启停操作。
 */
public interface SubsystemClient {

    /**
     * 子系统列表（2.3.7）：QSYS2.SUBSYSTEM_INFO。
     */
    List<SubsystemRow> listSubsystems();

    /** 启动子系统（STR subsystem NAME） */
    CommandResult startSubsystem(String name);

    /** 停止子系统（END subsystem NAME OPTION(*IMMED)） */
    CommandResult endSubsystem(String name);
}
