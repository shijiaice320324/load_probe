-- 告警记录：一次「触发 → 恢复」对应一行
CREATE TABLE IF NOT EXISTS alerts (
    id             BIGSERIAL PRIMARY KEY,
    rule_id        TEXT             NOT NULL,
    metric         TEXT             NOT NULL,
    level          TEXT             NOT NULL,
    status         TEXT             NOT NULL,
    host           TEXT             NOT NULL,
    value          DOUBLE PRECISION,
    threshold      DOUBLE PRECISION,
    message        TEXT,
    started_at     TIMESTAMPTZ      NOT NULL,
    resolved_at    TIMESTAMPTZ,
    notified_at    TIMESTAMPTZ,
    notify_channel TEXT,
    notify_error   TEXT
);

CREATE INDEX IF NOT EXISTS idx_alerts_started_at ON alerts (started_at DESC);
CREATE INDEX IF NOT EXISTS idx_alerts_status ON alerts (status);
CREATE INDEX IF NOT EXISTS idx_alerts_rule ON alerts (rule_id, started_at DESC);

COMMENT ON COLUMN alerts.status IS 'FIRING=触发中, RESOLVED=已恢复';
COMMENT ON COLUMN alerts.value IS '触发或恢复时刻的指标值';
