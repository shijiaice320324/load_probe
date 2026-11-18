package com.example.loadprobe.alert;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 规则判定：连续 N 个采样点超过阈值才触发，回落后自动恢复。
 * 状态保存在内存中，进程重启会重置（会重新经历一次连续计数）。
 */
@Component
public class AlertEvaluator {

    private static final Logger log = LoggerFactory.getLogger(AlertEvaluator.class);

    private final AlertProperties properties;
    private final Map<String, RuleState> states = new ConcurrentHashMap<>();

    public AlertEvaluator(AlertProperties properties) {
        this.properties = properties;
    }

    public List<AlertEvent> evaluate(MetricsSnapshot snapshot, Instant at) {
        List<AlertEvent> events = new ArrayList<>();
        if (!properties.isEnabled()) {
            return events;
        }
        for (AlertProperties.Rule rule : properties.getRules()) {
            double value = snapshot.valueOf(rule.getMetric());
            if (Double.isNaN(value)) {
                continue;
            }
            RuleState state = states.computeIfAbsent(rule.getId(), key -> new RuleState());
            AlertEvent event = evaluateRule(rule, value, at, state);
            if (event != null) {
                events.add(event);
            }
        }
        return events;
    }

    private AlertEvent evaluateRule(AlertProperties.Rule rule, double value, Instant at, RuleState state) {
        synchronized (state) {
            if (value >= rule.getThreshold()) {
                state.consecutive++;
                if (!state.firing && state.consecutive >= rule.getConsecutiveSamples()) {
                    state.firing = true;
                    state.since = at;
                    boolean cooled = state.lastNotified == null
                            || Duration.between(state.lastNotified, at).toMinutes() >= properties.getCooldownMinutes();
                    if (cooled) {
                        state.lastNotified = at;
                    }
                    log.info("告警触发 rule={} value={} threshold={} level={} notify={}",
                            rule.getId(), value, rule.getThreshold(), rule.getLevel(), cooled);
                    return new AlertEvent(rule, AlertStatus.FIRING, rule.getLevel(), rule.getMetric(),
                            value, rule.getThreshold(), at, cooled, describe(rule, value));
                }
                return null;
            }

            state.consecutive = 0;
            if (state.firing) {
                state.firing = false;
                state.since = null;
                log.info("告警恢复 rule={} value={} threshold={}", rule.getId(), value, rule.getThreshold());
                return new AlertEvent(rule, AlertStatus.RESOLVED, rule.getLevel(), rule.getMetric(),
                        value, rule.getThreshold(), at, true, describe(rule, value));
            }
            return null;
        }
    }

    private String describe(AlertProperties.Rule rule, double value) {
        String base = rule.getMessage() == null || rule.getMessage().isBlank() ? rule.getName() : rule.getMessage();
        return "%s（当前 %.2f，阈值 %.2f）".formatted(base, value, rule.getThreshold());
    }

    /** 供测试重置内部状态。 */
    void reset() {
        states.clear();
    }

    private static final class RuleState {
        int consecutive;
        boolean firing;
        Instant since;
        Instant lastNotified;
    }
}
