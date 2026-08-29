package com.rxas400adm.email.service;

import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.email.dto.EmailGroupCreateDTO;
import com.rxas400adm.email.dto.EmailRecipientDTO;
import com.rxas400adm.email.entity.EmailRecipient;
import com.rxas400adm.email.vo.EmailGroupVO;

import java.util.List;

public interface IEmailGroupService {

    PageResult<EmailGroupVO> page(int current, int size, String keyword);

    List<EmailGroupVO> listAll();

    void create(EmailGroupCreateDTO dto);

    void update(Long id, EmailGroupCreateDTO dto);

    void delete(Long id);

    List<EmailRecipient> members(Long id);

    void addMember(Long id, EmailRecipientDTO dto);

    void removeMember(Long id, Long memberId);
}
