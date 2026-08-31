package com.rxas400adm.email.service;

import com.rxas400adm.common.security.SecretMasker;
import com.rxas400adm.email.MailMessage;
import com.rxas400adm.email.entity.EmailConfig;
import com.rxas400adm.email.entity.EmailLog;
import com.rxas400adm.email.mapper.EmailConfigMapper;
import com.rxas400adm.email.mapper.EmailLogMapper;
import com.rxas400adm.system.service.ISysConfigService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.rxas400adm.email.vo.EmailConfigVO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 邮件服务实现：SMTP 配置优先从 rx_email_config 读取，回退到 rx_config（兼容旧数据）。
 * 发送结果记录到 rx_email_log。发送失败不抛异常（优雅降级）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService implements IEmailService {

    private static final String DEFAULT_SMTP_PORT = "465";
    private static final String DEFAULT_SMTP_TIMEOUT = "5000";

    private final EmailConfigMapper emailConfigMapper;
    private final EmailLogMapper emailLogMapper;
    private final ISysConfigService sysConfigService;

    @Override
    public void send(MailMessage message) {
        String recipients = message.recipients();
        if (recipients == null || recipients.isBlank()) {
            log.warn("[{}] 未配置收件人，跳过", message.channel());
            return;
        }
        String host = getConfig("host", "");
        if (host.isBlank()) {
            log.debug("[{}] 未配置 SMTP（host），跳过", message.channel());
            return;
        }

        int portNum;
        try {
            portNum = Integer.parseInt(getConfig("port", DEFAULT_SMTP_PORT));
        } catch (NumberFormatException nfe) {
            log.error("SMTP 端口配置非法={}，邮件通知禁用", getConfig("port", DEFAULT_SMTP_PORT));
            recordLog(message.subject(), recipients, message.channel(), "FAILED", "端口配置非法", message.filename());
            return;
        }

        String user = getConfig("user", "");
        String pass = getConfig("pass", "");
        String from = (message.sender() != null && !message.sender().isBlank())
                ? message.sender()
                : getConfig("from", user.isBlank() ? "rxas400adm@localhost" : user);

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(portNum);
        sender.setUsername(user);
        sender.setPassword(pass);
        sender.setDefaultEncoding("UTF-8");

        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", user.isBlank() ? "false" : "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.enable", DEFAULT_SMTP_PORT.equals(getConfig("port", DEFAULT_SMTP_PORT)) ? "true" : "false");
        String timeout = getConfig("timeout", DEFAULT_SMTP_TIMEOUT);
        props.put("mail.smtp.connectiontimeout", timeout);
        props.put("mail.smtp.timeout", timeout);

        try {
            MimeMessage mime = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, message.data() != null, "UTF-8");
            helper.setFrom(from);
            helper.setTo(recipients.split("[;, ]+"));
            helper.setSubject(message.subject());
            helper.setText(message.text(), false);

            // 设置优先级
            if ("HIGH".equalsIgnoreCase(message.priority())) {
                mime.setHeader("X-Priority", "1");
                mime.setHeader("X-MSMail-Priority", "High");
                mime.setHeader("Importance", "High");
            } else if ("LOW".equalsIgnoreCase(message.priority())) {
                mime.setHeader("X-Priority", "5");
                mime.setHeader("X-MSMail-Priority", "Low");
                mime.setHeader("Importance", "Low");
            }

            if (message.data() != null && message.data().length > 0 && message.filename() != null) {
                String contentType = message.filename().toLowerCase().endsWith(".pdf")
                        ? "application/pdf"
                        : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                helper.addAttachment(message.filename(), new ByteArrayResource(message.data()), contentType);
            }

            sender.send(mime);
            log.info("[{}] 已发送至 {}（{}）", message.channel(), recipients, message.subject());
            recordLog(message.subject(), recipients, message.channel(), "SUCCESS", null, message.filename());
        } catch (Exception e) {
            log.warn("[{}] 发送失败: {}", message.channel(), e.getMessage());
            recordLog(message.subject(), recipients, message.channel(), "FAILED", e.getMessage(), message.filename());
        }
    }

    @Override
    public void sendTestEmail(String to) {
        MailMessage testMsg = new MailMessage(
                "RXAS400 邮件测试",
                "这是一封测试邮件，如果您收到此邮件说明 SMTP 配置正确。",
                to, MailMessage.PRIORITY_NORMAL, null, null, MailMessage.CHANNEL_MANUAL, null
        );
        send(testMsg);
    }

    /**
     * 批量更新邮件配置（写入 rx_email_config 表，密码字段自动脱敏保护）。
     */
    public void updateConfigs(Map<String, String> configs) {
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (SecretMasker.isMasked(value)) {
                continue; // 前端未修改，保留旧值
            }
            EmailConfig existing = emailConfigMapper.selectById(key);
            if (existing != null) {
                existing.setConfigValue(value);
                emailConfigMapper.updateById(existing);
            } else {
                EmailConfig config = new EmailConfig();
                config.setConfigKey(key);
                config.setConfigValue(value);
                emailConfigMapper.insert(config);
            }
        }
    }

    @Override
    public List<EmailConfigVO> listConfigs() {
        return emailConfigMapper.selectList(null)
                .stream().map(EmailConfigVO::from).toList();
    }

    /**
     * 读取邮件配置：优先 rx_email_config，回退 rx_config（兼容旧数据）。
     */
    private String getConfig(String key, String defaultValue) {
        // 1. 优先从 rx_email_config 读取
        EmailConfig config = emailConfigMapper.selectById(key);
        if (config != null && config.getConfigValue() != null && !config.getConfigValue().isBlank()) {
            return config.getConfigValue();
        }
        // 2. 回退到 rx_config（兼容 alert.email.* 旧数据）
        return sysConfigService.get("alert.email." + key, defaultValue);
    }

    private void recordLog(String subject, String recipients, String channel,
                           String status, String errorMessage, String attachmentName) {
        try {
            EmailLog logEntry = new EmailLog();
            logEntry.setSubject(subject);
            logEntry.setRecipients(recipients);
            logEntry.setChannel(channel);
            logEntry.setStatus(status);
            logEntry.setErrorMessage(errorMessage);
            logEntry.setAttachmentName(attachmentName);
            logEntry.setCreatedTime(LocalDateTime.now());
            emailLogMapper.insert(logEntry);
        } catch (Exception e) {
            log.debug("记录邮件日志失败: {}", e.getMessage());
        }
    }
}