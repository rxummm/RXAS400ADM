package com.rxas400adm.email.service;

import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.email.vo.EmailLogVO;

public interface IEmailLogService {

    PageResult<EmailLogVO> page(int current, int size, String channel, String status, String keyword);

    EmailLogVO detail(Long id);
}
