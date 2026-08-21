package com.rxas400adm.as400.service;

import com.rxas400adm.as400.CommandResult;
import com.rxas400adm.as400.dto.CommandScriptRequest;
import com.rxas400adm.as400.entity.CommandScript;

import java.util.List;

/**
 * 命令脚本中心：保存/复用 CL 命令，收藏与标签分类，一键对指定服务器执行。
 */
public interface ICommandScriptService {

    List<CommandScript> list(Boolean favorite, String tag);

    List<String> tags();

    CommandScript create(CommandScriptRequest request, String username);

    CommandScript update(Long id, CommandScriptRequest request);

    void delete(Long id);

    CommandScript toggleFavorite(Long id, Boolean favorite);

    CommandResult execute(Long id, Long serverId);
}
