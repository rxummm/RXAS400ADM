package com.rxas400adm.email.service;

import com.rxas400adm.email.MailMessage;
import com.rxas400adm.email.vo.EmailConfigVO;

import java.util.List;
import java.util.Map;

/**
 * 邮件服务接口：统一邮件发送入口，替代原 EmailNotifier。
 */
public interface IEmailService {

    /** 发送邮件（统一入口） */
    void send(MailMessage message);

    /** 发送测试邮件 */
    void sendTestEmail(String to);

    /** 批量更新邮件配置 */
    void updateConfigs(Map<String, String> configs);

    /** 查询全部邮件配置 */
    List<EmailConfigVO> listConfigs();
}
