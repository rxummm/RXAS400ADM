package com.rxas400adm.as400;

import com.rxas400adm.as400.model.MessageDescriptor;
import com.rxas400adm.as400.model.MessageFileRow;
import com.rxas400adm.as400.model.MessageRow;

import java.util.List;

/**
 * 消息文件（*MSGF）域：文件列表、消息描述增删改查。
 */
public interface MessageFileClient {

    /**
     * 消息文件（*MSGF）列表（QSYS2.MESSAGE_FILE_INFO）。
     */
    List<MessageFileRow> listMessageFiles(String library);

    /**
     * 消息文件内的消息描述（QSYS2.MESSAGE_DESCRIPTION_INFO）；keyword 空则不过滤。
     */
    List<MessageRow> listMessages(String library, String file, String keyword);

    /** 新增消息描述（ADDMSGD MSGID MSGF MSG SECLVL SEV） */
    CommandResult addMessage(MessageDescriptor msg);

    /** 修改消息描述（CHGMSGD，按 MSGID 定位） */
    CommandResult updateMessage(MessageDescriptor msg);

    /** 删除消息描述（RMVMSGD MSGID MSGF） */
    CommandResult deleteMessage(String library, String file, String id);
}
