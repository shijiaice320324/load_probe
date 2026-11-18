package com.example.loadprobe.alert;

import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 告警规则查询与修改（需 X-API-Key）。 */
@RestController
@RequestMapping("/api/v1/alert-rules")
public class AlertRulesController {

    private final AlertRuleService ruleService;
    private final AlertEvaluator evaluator;

    public AlertRulesController(AlertRuleService ruleService, AlertEvaluator evaluator) {
        this.ruleService = ruleService;
        this.evaluator = evaluator;
    }

    @GetMapping
    public List<RuleView> list() {
        return ruleService.rules().stream().map(RuleView::from).toList();
    }

    @PutMapping("/{id}")
    public RuleView update(@PathVariable String id, @RequestBody UpdateRuleRequest request) {
        AlertRuleDef updated = ruleService.update(id, request.threshold(), request.consecutiveSamples(),
                request.enabled());
        // 阈值变了，旧的连续计数不再有意义
        evaluator.resetState();
        return RuleView.from(updated);
    }

    public record UpdateRuleRequest(Double threshold, Integer consecutiveSamples, Boolean enabled) {
    }

    public record RuleView(String id, String name, String metric, double threshold, int consecutiveSamples,
                           String level, String message, boolean enabled) {
        static RuleView from(AlertRuleDef rule) {
            return new RuleView(rule.id(), rule.name(), rule.metric().name(), rule.threshold(),
                    rule.consecutiveSamples(), rule.level().name(), rule.message(), rule.enabled());
        }
    }

    /** 便于前端显示“最近刷新时间”。 */
    public Instant now() {
        return Instant.now();
    }
}
