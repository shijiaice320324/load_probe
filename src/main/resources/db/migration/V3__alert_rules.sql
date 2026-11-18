-- 告警规则（前端可修改；启动时若为空则用 application.yml 的默认值初始化）
CREATE TABLE IF NOT EXISTS alert_rules (
    id                  TEXT PRIMARY KEY,
    name                TEXT             NOT NULL,
    metric              TEXT             NOT NULL,
    threshold           DOUBLE PRECISION NOT NULL,
    consecutive_samples INTEGER          NOT NULL,
    level               TEXT             NOT NULL,
    message             TEXT,
    enabled             BOOLEAN          NOT NULL DEFAULT TRUE,
    updated_at          TIMESTAMPTZ      NOT NULL DEFAULT now()
);

COMMENT ON COLUMN alert_rules.id IS '规则标识，如 cpu-high';
COMMENT ON COLUMN alert_rules.metric IS 'CPU / MEM / DISK / COLLECT_AGE_SECONDS';
COMMENT ON COLUMN alert_rules.threshold IS '百分比指标用 %，断档用秒';
