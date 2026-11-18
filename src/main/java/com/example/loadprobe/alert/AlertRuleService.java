package com.example.loadprobe.alert;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 规则来源：以数据库为准，启动时把 application.yml 里缺失的规则补进去（已有规则保留用户修改）。
 * 修改后立即刷新缓存，下一轮采集即生效。
 */
@Service
public class AlertRuleService implements AlertRuleProvider {

    private static final Logger log = LoggerFactory.getLogger(AlertRuleService.class);

    private final AlertRuleRepository repository;
    private final AlertProperties properties;
    private volatile List<AlertRuleDef> cache = List.of();

    public AlertRuleService(AlertRuleRepository repository, AlertProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    @PostConstruct
    void seedAndReload() {
        try {
            for (AlertProperties.Rule rule : properties.getRules()) {
                if (!repository.exists(rule.getId())) {
                    repository.insert(AlertRuleDef.from(rule));
                    log.info("初始化告警规则 {}", rule.getId());
                }
            }
        } catch (Exception e) {
            log.error("初始化告警规则失败: {}", e.toString(), e);
        }
        reload();
    }

    @Override
    public List<AlertRuleDef> rules() {
        if (cache.isEmpty()) {
            reload();
        }
        return cache;
    }

    public synchronized void reload() {
        try {
            cache = repository.findAll();
        } catch (Exception e) {
            log.error("读取告警规则失败: {}", e.toString(), e);
        }
    }

    /** 更新阈值/连续样本/开关；返回更新后的规则。 */
    public AlertRuleDef update(String id, Double threshold, Integer consecutiveSamples, Boolean enabled) {
        AlertRuleDef current = repository.findAll().stream()
                .filter(rule -> rule.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("规则不存在: " + id));

        double newThreshold = threshold == null ? current.threshold() : threshold;
        int newConsecutive = consecutiveSamples == null ? current.consecutiveSamples() : consecutiveSamples;
        boolean newEnabled = enabled == null ? current.enabled() : enabled;

        validate(current, newThreshold, newConsecutive);

        AlertRuleDef updated = new AlertRuleDef(current.id(), current.name(), current.metric(),
                newThreshold, newConsecutive, current.level(), current.message(), newEnabled);
        repository.update(updated);
        reload();
        log.info("告警规则已更新: {} threshold={} consecutive={} enabled={}",
                id, newThreshold, newConsecutive, newEnabled);
        return updated;
    }

    private void validate(AlertRuleDef rule, double threshold, int consecutive) {
        if (consecutive < 1 || consecutive > 100) {
            throw new IllegalArgumentException("连续样本数取值范围 1..100");
        }
        if (rule.metric() == AlertMetric.COLLECT_AGE_SECONDS) {
            if (threshold < 5 || threshold > 86400) {
                throw new IllegalArgumentException("断档阈值取值范围 5..86400 秒");
            }
        } else if (threshold <= 0 || threshold > 100) {
            throw new IllegalArgumentException("百分比阈值取值范围 0..100");
        }
    }

    /** 便于在日志/接口里展示规则最后更新时间。 */
    public Instant now() {
        return Instant.now();
    }
}
