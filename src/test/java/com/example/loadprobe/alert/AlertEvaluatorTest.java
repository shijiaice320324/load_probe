package com.example.loadprobe.alert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AlertEvaluator 告警判定")
class AlertEvaluatorTest {

    private AlertProperties properties;
    private AlertEvaluator evaluator;
    private final Instant t0 = Instant.parse("2026-11-18T08:00:00Z");

    @BeforeEach
    void setUp() {
        properties = new AlertProperties();
        properties.setEnabled(true);
        properties.setCooldownMinutes(15);
        properties.setRules(List.of(cpuRule()));
        evaluator = new AlertEvaluator(properties);
    }

    private AlertProperties.Rule cpuRule() {
        AlertProperties.Rule rule = new AlertProperties.Rule();
        rule.setId("cpu-high");
        rule.setName("CPU 使用率过高");
        rule.setMetric(AlertMetric.CPU);
        rule.setThreshold(90);
        rule.setConsecutiveSamples(3);
        rule.setLevel(AlertLevel.CRITICAL);
        return rule;
    }

    @Test
    @DisplayName("连续 3 次超过阈值才触发")
    void firesOnlyAfterConsecutiveSamples() {
        assertTrue(evaluator.evaluate(MetricsSnapshot.ofCollection(95, 50, 20), t0).isEmpty());
        assertTrue(evaluator.evaluate(MetricsSnapshot.ofCollection(95, 50, 20), t0.plusSeconds(30)).isEmpty());

        List<AlertEvent> events = evaluator.evaluate(MetricsSnapshot.ofCollection(93, 50, 20), t0.plusSeconds(60));
        assertEquals(1, events.size());
        AlertEvent event = events.get(0);
        assertEquals(AlertStatus.FIRING, event.status());
        assertEquals(AlertLevel.CRITICAL, event.level());
        assertEquals(93, event.value(), 0.001);
        assertTrue(event.notifyEnabled(), "首次触发应当通知");
    }

    @Test
    @DisplayName("中途回落会清零连续计数")
    void resetsCounterWhenValueDrops() {
        evaluator.evaluate(MetricsSnapshot.ofCollection(95, 50, 20), t0);
        evaluator.evaluate(MetricsSnapshot.ofCollection(95, 50, 20), t0.plusSeconds(30));
        assertTrue(evaluator.evaluate(MetricsSnapshot.ofCollection(10, 50, 20), t0.plusSeconds(60)).isEmpty());

        assertTrue(evaluator.evaluate(MetricsSnapshot.ofCollection(95, 50, 20), t0.plusSeconds(90)).isEmpty());
        assertTrue(evaluator.evaluate(MetricsSnapshot.ofCollection(95, 50, 20), t0.plusSeconds(120)).isEmpty());
        assertEquals(1, evaluator.evaluate(MetricsSnapshot.ofCollection(95, 50, 20), t0.plusSeconds(150)).size());
    }

    @Test
    @DisplayName("回落后产生恢复事件")
    void emitsResolvedEvent() {
        evaluator.evaluate(MetricsSnapshot.ofCollection(95, 50, 20), t0);
        evaluator.evaluate(MetricsSnapshot.ofCollection(95, 50, 20), t0.plusSeconds(30));
        evaluator.evaluate(MetricsSnapshot.ofCollection(95, 50, 20), t0.plusSeconds(60));

        List<AlertEvent> events = evaluator.evaluate(MetricsSnapshot.ofCollection(40, 50, 20), t0.plusSeconds(90));
        assertEquals(1, events.size());
        assertEquals(AlertStatus.RESOLVED, events.get(0).status());
        assertEquals(40, events.get(0).value(), 0.001);
    }

    @Test
    @DisplayName("冷却期内再次触发只落库不通知")
    void appliesCooldown() {
        evaluator.evaluate(MetricsSnapshot.ofCollection(95, 50, 20), t0);
        evaluator.evaluate(MetricsSnapshot.ofCollection(95, 50, 20), t0.plusSeconds(30));
        evaluator.evaluate(MetricsSnapshot.ofCollection(95, 50, 20), t0.plusSeconds(60));
        evaluator.evaluate(MetricsSnapshot.ofCollection(10, 50, 20), t0.plusSeconds(90));

        // 冷却期内（1 分钟后）再次触发
        Instant t1 = t0.plusSeconds(60 + 60);
        evaluator.evaluate(MetricsSnapshot.ofCollection(95, 50, 20), t1);
        evaluator.evaluate(MetricsSnapshot.ofCollection(95, 50, 20), t1.plusSeconds(30));
        List<AlertEvent> again = evaluator.evaluate(MetricsSnapshot.ofCollection(95, 50, 20), t1.plusSeconds(60));
        assertEquals(1, again.size());
        assertEquals(AlertStatus.FIRING, again.get(0).status());
        assertFalse(again.get(0).notifyEnabled(), "冷却期内不应重复发通知");
    }

    @Test
    @DisplayName("断档规则一次即触发")
    void triggersStaleRuleImmediately() {
        AlertProperties.Rule stale = new AlertProperties.Rule();
        stale.setId("collect-stale");
        stale.setName("采集断档");
        stale.setMetric(AlertMetric.COLLECT_AGE_SECONDS);
        stale.setThreshold(180);
        stale.setConsecutiveSamples(1);
        stale.setLevel(AlertLevel.CRITICAL);
        properties.setRules(List.of(stale));
        evaluator = new AlertEvaluator(properties);

        assertTrue(evaluator.evaluate(MetricsSnapshot.ofCollectAge(120), t0).isEmpty());
        List<AlertEvent> events = evaluator.evaluate(MetricsSnapshot.ofCollectAge(200), t0.plusSeconds(30));
        assertEquals(1, events.size());
        assertEquals(AlertMetric.COLLECT_AGE_SECONDS, events.get(0).metric());
    }

    @Test
    @DisplayName("总开关关闭时不产生任何事件")
    void disabledProducesNothing() {
        properties.setEnabled(false);
        assertTrue(evaluator.evaluate(MetricsSnapshot.ofCollection(99, 99, 99), t0).isEmpty());
    }
}
