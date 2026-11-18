package com.example.loadprobe.alert;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/** 告警记录读写。 */
@Repository
public class AlertRepository {

    private final JdbcTemplate jdbc;

    public AlertRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long insert(AlertEvent event, String host) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var ps = connection.prepareStatement("""
                    INSERT INTO alerts (rule_id, metric, level, status, host, value, threshold,
                        message, started_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, new String[]{"id"});
            ps.setString(1, event.rule().id());
            ps.setString(2, event.metric().name());
            ps.setString(3, event.level().name());
            ps.setString(4, event.status().name());
            ps.setString(5, host);
            ps.setDouble(6, event.value());
            ps.setDouble(7, event.threshold());
            ps.setString(8, event.message());
            ps.setTimestamp(9, Timestamp.from(event.at()));
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? -1 : key.longValue();
    }

    public List<Long> findOpenIds(String ruleId) {
        return jdbc.queryForList(
                "SELECT id FROM alerts WHERE rule_id = ? AND status = 'FIRING' ORDER BY started_at DESC",
                Long.class, ruleId);
    }

    public int resolve(String ruleId, Instant resolvedAt) {
        return jdbc.update("""
                UPDATE alerts SET status = 'RESOLVED', resolved_at = ?
                WHERE rule_id = ? AND status = 'FIRING'
                """, Timestamp.from(resolvedAt), ruleId);
    }

    /** 服务重启后内存状态丢失，把上次遗留的 FIRING 记录统一关闭，避免永远显示“触发中”。 */
    public int resolveAllFiring(Instant resolvedAt) {
        return jdbc.update("""
                UPDATE alerts SET status = 'RESOLVED', resolved_at = ?,
                       notify_error = coalesce(notify_error, '服务重启，状态自动关闭')
                WHERE status = 'FIRING'
                """, Timestamp.from(resolvedAt));
    }

    public void markNotified(long alertId, String channel) {
        if ("log".equals(channel)) {
            // 日志只是兜底，不覆盖真实渠道的失败原因
            jdbc.update("UPDATE alerts SET notified_at = now(), notify_channel = coalesce(notify_channel, ?) WHERE id = ?",
                    channel, alertId);
        } else {
            jdbc.update("UPDATE alerts SET notified_at = now(), notify_channel = ?, notify_error = NULL WHERE id = ?",
                    channel, alertId);
        }
    }

    public void markNotifyFailed(long alertId, String error) {
        jdbc.update("UPDATE alerts SET notify_error = ? WHERE id = ?",
                error == null ? "unknown" : error.substring(0, Math.min(500, error.length())), alertId);
    }

    public List<AlertRow> query(Instant from, Instant to, String level, String status, int limit) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, rule_id, metric, level, status, host, value, threshold, message,
                       started_at, resolved_at, notified_at, notify_channel
                FROM alerts WHERE started_at >= ? AND started_at < ?
                """);
        if (level != null && !level.isBlank()) {
            sql.append(" AND level = ?");
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
        }
        sql.append(" ORDER BY started_at DESC LIMIT ?");

        List<Object> args = new java.util.ArrayList<>();
        args.add(Timestamp.from(from));
        args.add(Timestamp.from(to));
        if (level != null && !level.isBlank()) {
            args.add(level.toUpperCase());
        }
        if (status != null && !status.isBlank()) {
            args.add(status.toUpperCase());
        }
        args.add(limit);

        return jdbc.query(sql.toString(), (rs, rowNum) -> new AlertRow(
                rs.getLong("id"),
                rs.getString("rule_id"),
                rs.getString("metric"),
                rs.getString("level"),
                rs.getString("status"),
                rs.getString("host"),
                rs.getDouble("value"),
                rs.getDouble("threshold"),
                rs.getString("message"),
                rs.getTimestamp("started_at") == null ? null : rs.getTimestamp("started_at").toInstant(),
                rs.getTimestamp("resolved_at") == null ? null : rs.getTimestamp("resolved_at").toInstant(),
                rs.getTimestamp("notified_at") == null ? null : rs.getTimestamp("notified_at").toInstant(),
                rs.getString("notify_channel")), args.toArray());
    }

    public record AlertRow(long id, String ruleId, String metric, String level, String status, String host,
                           double value, double threshold, String message,
                           Instant startedAt, Instant resolvedAt, Instant notifiedAt, String notifyChannel) {
    }
}
