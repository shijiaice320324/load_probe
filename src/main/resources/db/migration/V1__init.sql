-- 系统级指标：每轮采集一行
CREATE TABLE IF NOT EXISTS system_metrics (
    id                    BIGSERIAL PRIMARY KEY,
    collected_at          TIMESTAMPTZ      NOT NULL,
    host                  TEXT             NOT NULL,
    cpu_usage_pct         DOUBLE PRECISION NOT NULL,
    mem_total_bytes       BIGINT           NOT NULL,
    mem_used_bytes        BIGINT           NOT NULL,
    mem_usage_pct         DOUBLE PRECISION NOT NULL,
    disk_total_bytes      BIGINT,
    disk_used_bytes       BIGINT,
    disk_usage_pct        DOUBLE PRECISION,
    net_rx_bytes_per_sec  DOUBLE PRECISION,
    net_tx_bytes_per_sec  DOUBLE PRECISION
);

CREATE INDEX IF NOT EXISTS idx_system_metrics_collected_at
    ON system_metrics (collected_at DESC);

COMMENT ON COLUMN system_metrics.collected_at IS '采集时刻（UTC 存储）';
COMMENT ON COLUMN system_metrics.net_rx_bytes_per_sec IS '两次采样之间的平均接收速率';

-- 进程级指标：每轮最多写入 Top-N 行
CREATE TABLE IF NOT EXISTS process_metrics (
    id              BIGSERIAL PRIMARY KEY,
    collected_at    TIMESTAMPTZ      NOT NULL,
    host            TEXT             NOT NULL,
    pid             BIGINT           NOT NULL,
    process_name    TEXT             NOT NULL,
    cpu_usage_pct   DOUBLE PRECISION,
    mem_rss_bytes   BIGINT,
    mem_usage_pct   DOUBLE PRECISION
);

CREATE INDEX IF NOT EXISTS idx_process_metrics_collected_at
    ON process_metrics (collected_at DESC);
CREATE INDEX IF NOT EXISTS idx_process_metrics_ts_pid
    ON process_metrics (collected_at DESC, pid);
