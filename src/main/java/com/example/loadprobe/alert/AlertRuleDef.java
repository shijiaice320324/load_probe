package com.example.loadprobe.alert;

/** 运行时告警规则（来自数据库，可由前端修改）。 */
public record AlertRuleDef(
        String id,
        String name,
        AlertMetric metric,
        double threshold,
        int consecutiveSamples,
        AlertLevel level,
        String message,
        boolean enabled) {

    public static AlertRuleDef from(AlertProperties.Rule rule) {
        return new AlertRuleDef(rule.getId(), rule.getName(), rule.getMetric(), rule.getThreshold(),
                rule.getConsecutiveSamples(), rule.getLevel(), rule.getMessage(), true);
    }
}
