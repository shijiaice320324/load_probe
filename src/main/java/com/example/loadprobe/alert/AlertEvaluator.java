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
 * 规则来自 {@link AlertRuleProvider}（数据库，可由前端修改），状态保存在内存中。
 */
@Component
public class AlertEvaluator {

    private static final Logger log = LoggerFactory.getLogger(AlertEvaluator.class);

    private final AlertProperties properties;
    private final AlertRuleProvider ruleProvider;
    private final Map<String, RuleState> states = new ConcurrentHashMap<>();

    public AlertEvaluator(AlertProperties properties, AlertRuleProvider ruleProvider) {
        this.properties = properties;
        this.ruleProvider = ruleProvider;
    }

    public List<AlertEvent> evaluate(MetricsSnapshot snapshot, Instant at) {
        List<AlertEvent> events = new ArrayList<>();
        if (!properties.isEnabled()) {
            return events;
        }
        for (AlertRuleDef rule : ruleProvider.rules()) {
            if (!rule.enabled()) {
                continue;
            }
            double value = snapshot.valueOf(rule.metric());
            if (Double.isNaN(value)) {
                continue;
            }
            RuleState state = states.computeIfAbsent(rule.id(), key -> new RuleState());
            AlertEvent event = evaluateRule(rule, value, at, state);
            if (event != null) {
                events.add(event);
            }
        }
        return events;
    }

    private AlertEvent evaluateRule(AlertRuleDef rule, double value, Instant at, RuleState state) {
        synchronized (state) {
            if (value >= rule.threshold()) {
                state.consecutive++;
                if (!state.firing && state.consecutive >= rule.consecutiveSamples()) {
                    state.firing = true;
                    state.since = at;
                    boolean cooled = state.lastNotified == null
                            || Duration.between(state.lastNotified, at).toMinutes() >= properties.getCooldownMinutes();
                    if (cooled) {
                        state.lastNotified = at;
                    }
                    log.info("告警触发 rule={} value={} threshold={} level={} notify={}",
                            rule.id(), value, rule.threshold(), rule.level(), cooled);
                    return new AlertEvent(rule, AlertStatus.FIRING, rule.level(), rule.metric(),
                            value, rule.threshold(), at, cooled, describe(rule, value));
                }
                return null;
            }

            state.consecutive = 0;
            if (state.firing) {
                state.firing = false;
                state.since = null;
                log.info("告警恢复 rule={} value={} threshold={}", rule.id(), value, rule.threshold());
                return new AlertEvent(rule, AlertStatus.RESOLVED, rule.level(), rule.metric(),
                        value, rule.threshold(), at, true, describe(rule, value));
            }
            return null;
        }
    }

    private String describe(AlertRuleDef rule, double value) {
        String base = rule.message() == null || rule.message().isBlank() ? rule.name() : rule.message();
        return "%s（当前 %.2f，阈值 %.2f）".formatted(base, value, rule.threshold());
    }

    /** 规则被前端修改后，清空内存状态，让新阈值立即重新计数。 */
    public void resetState() {
        states.clear();
    }

    private static final class RuleState {
        int consecutive;
        boolean firing;
        Instant since;
        Instant lastNotified;
    }
}
