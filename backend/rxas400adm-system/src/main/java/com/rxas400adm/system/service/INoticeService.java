package com.rxas400adm.system.service;

import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.system.dto.NoticeDTO;
import com.rxas400adm.system.entity.Notice;

import java.util.List;

/**
 * 系统公告服务接口（rx_notice）。
 */
public interface INoticeService {

    List<Notice> published();

    PageResult<Notice> page(int current, int size, String keyword, Integer status);

    Notice create(NoticeDTO notice, String username);

    Notice update(Long id, NoticeDTO dto);

    void delete(Long id);
}
