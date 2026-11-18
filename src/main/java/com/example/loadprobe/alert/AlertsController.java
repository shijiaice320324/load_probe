package com.example.loadprobe.alert;

import com.example.loadprobe.alert.AlertRepository.AlertRow;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 告警查询接口（需 X-API-Key）。 */
@RestController
@RequestMapping("/api/v1")
public class AlertsController {

    private static final Logger log = LoggerFactory.getLogger(AlertsController.class);
    private static final int MAX_LIMIT = 500;

    private final AlertRepository repository;

    public AlertsController(AlertRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/alerts")
    public AlertList alerts(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "100") int limit) {
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("limit 取值范围 1.." + MAX_LIMIT);
        }
        Instant toInstant = to == null || to.isBlank() ? Instant.now() : parse(to, "to");
        Instant fromInstant = from == null || from.isBlank()
                ? toInstant.minusSeconds(7 * 24 * 3600L)
                : parse(from, "from");
        if (!fromInstant.isBefore(toInstant)) {
            throw new IllegalArgumentException("from 必须早于 to");
        }
        List<AlertRow> rows = repository.query(fromInstant, toInstant, level, status, limit);
        log.debug("查询告警 from={} to={} level={} status={} rows={}",
                fromInstant, toInstant, level, status, rows.size());
        return new AlertList(fromInstant, toInstant, limit, rows.stream().map(AlertItem::from).toList());
    }

    private static Instant parse(String raw, String name) {
        try {
            return Instant.parse(raw);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(name + " 需为 ISO-8601 时间，例如 2026-11-18T00:00:00Z");
        }
    }

    public record AlertItem(long id, String ruleId, String metric, String level, String status, String host,
                            double value, double threshold, String message,
                            Instant startedAt, Instant resolvedAt, Instant notifiedAt, String notifyChannel) {
        static AlertItem from(AlertRow row) {
            return new AlertItem(row.id(), row.ruleId(), row.metric(), row.level(), row.status(), row.host(),
                    row.value(), row.threshold(), row.message(),
                    row.startedAt(), row.resolvedAt(), row.notifiedAt(), row.notifyChannel());
        }
    }

    public record AlertList(Instant from, Instant to, int limit, List<AlertItem> items) {
    }
}
