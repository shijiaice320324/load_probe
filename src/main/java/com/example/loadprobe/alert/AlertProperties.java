package com.example.loadprobe.alert;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** 告警配置，前缀 loadprobe.alerts。 */
@Validated
@ConfigurationProperties(prefix = "loadprobe.alerts")
public class AlertProperties {

    /** 总开关。 */
    private boolean enabled = true;

    /** 同一规则两次邮件通知的最小间隔（分钟）。 */
    @Min(1)
    private int cooldownMinutes = 15;

    /** 邮件收件人列表。 */
    private List<String> recipients = new ArrayList<>();

    /** 告警规则。 */
    private List<Rule> rules = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getCooldownMinutes() {
        return cooldownMinutes;
    }

    public void setCooldownMinutes(int cooldownMinutes) {
        this.cooldownMinutes = cooldownMinutes;
    }

    public List<String> getRecipients() {
        return recipients;
    }

    public void setRecipients(List<String> recipients) {
        this.recipients = recipients;
    }

    public List<Rule> getRules() {
        return rules;
    }

    public void setRules(List<Rule> rules) {
        this.rules = rules;
    }

    /** 单条规则。 */
    public static class Rule {
        @NotBlank
        private String id;
        @NotBlank
        private String name;
        private AlertMetric metric = AlertMetric.CPU;
        private double threshold;
        /** 连续多少个采样点超过阈值才触发，默认 1。 */
        @Min(1)
        private int consecutiveSamples = 1;
        private AlertLevel level = AlertLevel.CRITICAL;
        /** 触发时展示在通知里的描述。 */
        private String message;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public AlertMetric getMetric() {
            return metric;
        }

        public void setMetric(AlertMetric metric) {
            this.metric = metric;
        }

        public double getThreshold() {
            return threshold;
        }

        public void setThreshold(double threshold) {
            this.threshold = threshold;
        }

        public int getConsecutiveSamples() {
            return consecutiveSamples;
        }

        public void setConsecutiveSamples(int consecutiveSamples) {
            this.consecutiveSamples = consecutiveSamples;
        }

        public AlertLevel getLevel() {
            return level;
        }

        public void setLevel(AlertLevel level) {
            this.level = level;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
