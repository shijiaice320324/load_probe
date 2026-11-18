package com.example.loadprobe.alert;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** alert_rules 表读写。 */
@Repository
public class AlertRuleRepository {

    private final JdbcTemplate jdbc;

    public AlertRuleRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<AlertRuleDef> findAll() {
        return jdbc.query("""
                SELECT id, name, metric, threshold, consecutive_samples, level, message, enabled
                FROM alert_rules ORDER BY id
                """,
                (rs, rowNum) -> new AlertRuleDef(
                        rs.getString("id"),
                        rs.getString("name"),
                        AlertMetric.valueOf(rs.getString("metric")),
                        rs.getDouble("threshold"),
                        rs.getInt("consecutive_samples"),
                        AlertLevel.valueOf(rs.getString("level")),
                        rs.getString("message"),
                        rs.getBoolean("enabled")));
    }

    public boolean exists(String id) {
        Integer count = jdbc.queryForObject("SELECT count(*) FROM alert_rules WHERE id = ?", Integer.class, id);
        return count != null && count > 0;
    }

    public void insert(AlertRuleDef rule) {
        jdbc.update("""
                INSERT INTO alert_rules (id, name, metric, threshold, consecutive_samples, level, message, enabled)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, rule.id(), rule.name(), rule.metric().name(), rule.threshold(),
                rule.consecutiveSamples(), rule.level().name(), rule.message(), rule.enabled());
    }

    public int update(AlertRuleDef rule) {
        return jdbc.update("""
                UPDATE alert_rules
                SET threshold = ?, consecutive_samples = ?, enabled = ?, updated_at = ?
                WHERE id = ?
                """, rule.threshold(), rule.consecutiveSamples(), rule.enabled(),
                Timestamp.from(Instant.now()), rule.id());
    }
}
