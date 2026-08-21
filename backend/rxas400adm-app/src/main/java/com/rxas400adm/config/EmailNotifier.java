package com.rxas400adm.config;

import com.rxas400adm.system.service.ISysConfigService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.Properties;

/**
 * 邮件告警通知（2.1.5）：SMTP 配置来自 sys_config（alert.email.host/port/user/pass/to/from），\n * 未配置或发送失败仅记录日志（优雅降级，不阻塞告警链路）。\n */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotifier {

    private final ISysConfigService sysConfigService;

    public void sendAlert(String title, String message) {
        if (sysConfigService.get("alert.email.host", "").isBlank()) {
            return; // 未配置邮件，跳过
        }
        String to = sysConfigService.get("alert.email.to", "");
        send(title, message, to, null, null, null, "alert.email");
    }

    /**
     * 发送带附件邮件（报表定时推送用）：收件人来自任务配置 recipients（逗号/分号/空格分隔）。
     * 附件为 null 时退化为纯文本邮件。SMTP 配置统一读 alert.email.*。
     */
    public void sendAttachment(String title, String text, String recipients, String filename, byte[] data) {
        send(title, text, recipients, filename, data,
                filename != null && filename.toLowerCase().endsWith(".pdf")
                        ? "application/pdf" : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "report.email");
    }

    private void send(String title, String text, String recipients, String filename, byte[] data,
                      String contentType, String logTag) {
        String host = sysConfigService.get("alert.email.host", "");
        if (host.isBlank()) {
            log.debug("[{}] 未配置 SMTP（alert.email.host），跳过", logTag);
            return;
        }
        if (recipients == null || recipients.isBlank()) {
            log.warn("[{}] 未配置收件人，跳过", logTag);
            return;
        }
        String port = sysConfigService.get("alert.email.port", "465");
        String user = sysConfigService.get("alert.email.user", "");
        String pass = sysConfigService.get("alert.email.pass", "");
        String from = sysConfigService.get("alert.email.from", user.isBlank() ? "rxas400adm@localhost" : user);

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(Integer.parseInt(port));
        sender.setUsername(user);
        sender.setPassword(pass);
        sender.setDefaultEncoding("UTF-8");
        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", user.isBlank() ? "false" : "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.enable", "465".equals(port) ? "true" : "false");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");

        try {
            MimeMessage mime = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(recipients.split("[;, ]+"));
            helper.setSubject(title);
            helper.setText(text, false);
            if (data != null && data.length > 0 && filename != null) {
                helper.addAttachment(filename, new org.springframework.core.io.ByteArrayResource(data), contentType);
            }
            sender.send(mime);
            log.info("[{}] 已发送至 {}（{}）", logTag, recipients, title);
        } catch (Exception e) {
            log.warn("[{}] 发送失败: {}", logTag, e.getMessage());
        }
    }
}
