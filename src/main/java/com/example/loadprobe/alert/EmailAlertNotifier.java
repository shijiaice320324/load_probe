package com.example.loadprobe.alert;

import jakarta.mail.internet.MimeMessage;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/** 邮件通知：走 spring.mail.* 配置（SMTP 授权码放在环境变量里）。 */
@Component
public class EmailAlertNotifier implements AlertNotifier {

    private static final Logger log = LoggerFactory.getLogger(EmailAlertNotifier.class);
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final AlertProperties properties;
    private final String from;

    public EmailAlertNotifier(ObjectProvider<JavaMailSender> mailSenderProvider,
                              AlertProperties properties,
                              @Value("${spring.mail.username:}") String from) {
        this.mailSenderProvider = mailSenderProvider;
        this.properties = properties;
        this.from = from;
    }

    @Override
    public String channel() {
        return "email";
    }

    @Override
    public boolean isConfigured() {
        return mailSenderProvider.getIfAvailable() != null
                && from != null && !from.isBlank()
                && properties.getRecipients() != null && !properties.getRecipients().isEmpty();
    }

    @Override
    public void send(AlertEvent event, long alertId) throws Exception {
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            throw new IllegalStateException("未配置 spring.mail.host，无法发送邮件");
        }

        String statusText = event.status() == AlertStatus.FIRING ? "触发" : "恢复";
        String subject = "[load_probe] %s %s：%s（%.1f%%，阈值 %.1f%%）".formatted(
                event.status() == AlertStatus.FIRING ? "🔴" : "🟢",
                statusText, event.rule().name(), event.value(), event.threshold());

        String body = """
                load_probe 告警通知

                状态：%s
                级别：%s
                规则：%s（%s）
                指标：%s = %.2f（阈值 %.2f）
                主机：%s
                时间：%s
                说明：%s
                告警记录 ID：%d

                —— 本邮件由 load_probe 自动发送
                """.formatted(
                statusText,
                event.level(),
                event.rule().name(),
                event.rule().id(),
                event.metric(),
                event.value(),
                event.threshold(),
                System.getenv().getOrDefault("COMPUTERNAME", "unknown"),
                TIME_FORMAT.format(event.at()),
                event.message(),
                alertId);

        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
        helper.setFrom(from);
        helper.setTo(properties.getRecipients().toArray(new String[0]));
        helper.setSubject(subject);
        helper.setText(body, false);

        // 新浪 SMTP 偶尔握手/读取超时，简单重试一次
        int attempts = 2;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                sender.send(message);
                log.info("告警邮件已发送 alertId={} to={}（第 {} 次尝试）",
                        alertId, List.copyOf(properties.getRecipients()), attempt);
                return;
            } catch (Exception e) {
                if (attempt == attempts) {
                    throw e;
                }
                log.warn("第 {} 次发送告警邮件失败，2 秒后重试: {}", attempt, e.toString());
                Thread.sleep(2000);
            }
        }
    }
}
