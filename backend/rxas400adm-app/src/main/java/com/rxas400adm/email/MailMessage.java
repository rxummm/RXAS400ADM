package com.rxas400adm.email;

/**
 * 邮件消息统一对象：收敛邮件发送参数，替代原 EmailNotifier 的多方法签名。
 */
public record MailMessage(
        String subject,
        String text,
        String recipients,
        String priority,
        String filename,
        byte[] data,
        String channel
) {

    public static final String CHANNEL_ALERT = "ALERT";
    public static final String CHANNEL_REPORT = "REPORT";
    public static final String CHANNEL_MANUAL = "MANUAL";
    public static final String PRIORITY_NORMAL = "NORMAL";

    public static MailMessage alert(String subject, String text) {
        return new MailMessage(subject, text, null, PRIORITY_NORMAL, null, null, CHANNEL_ALERT);
    }

    public static MailMessage report(String subject, String text, String recipients,
                                     String filename, byte[] data) {
        return new MailMessage(subject, text, recipients, PRIORITY_NORMAL, filename, data, CHANNEL_REPORT);
    }

    public static MailMessage manual(String subject, String text, String recipients, String priority) {
        return new MailMessage(subject, text, recipients, priority, null, null, CHANNEL_MANUAL);
    }
}