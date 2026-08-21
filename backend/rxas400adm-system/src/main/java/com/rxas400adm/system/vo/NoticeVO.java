package com.rxas400adm.system.vo;

import com.rxas400adm.system.entity.Notice;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NoticeVO {

    private Long id;
    private String title;
    private String content;
    private Integer status;
    private LocalDateTime publishedTime;
    private LocalDateTime createdTime;

    public static NoticeVO from(Notice entity) {
        NoticeVO vo = new NoticeVO();
        vo.setId(entity.getId());
        vo.setTitle(entity.getTitle());
        vo.setContent(entity.getContent());
        vo.setStatus(entity.getStatus());
        vo.setPublishedTime(entity.getPublishedTime());
        vo.setCreatedTime(entity.getCreatedTime());
        return vo;
    }
}