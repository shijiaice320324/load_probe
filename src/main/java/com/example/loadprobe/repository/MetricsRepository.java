package com.example.loadprobe.repository;

import com.example.loadprobe.collector.ProcessSample;
import com.example.loadprobe.collector.SystemSample;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 指标读写。查询侧的聚合按"桶"计算，避免一次返回过密的数据点。 */
@Repository
public class MetricsRepository {

    private final JdbcTemplate jdbc;

    public MetricsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insertSystem(SystemSample sample, String host) {
        jdbc.update("""
                INSERT INTO system_metrics (collected_at, host, cpu_usage_pct,
                    mem_total_bytes, mem_used_bytes, mem_usage_pct,
                    disk_total_bytes, disk_used_bytes, disk_usage_pct,
                    net_rx_bytes_per_sec, net_tx_bytes_per_sec)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                Timestamp.from(sample.collectedAt()), host, sample.cpuUsagePct(),
                sample.memTotalBytes(), sample.memUsedBytes(), sample.memUsagePct(),
                sample.diskTotalBytes(), sample.diskUsedBytes(), sample.diskUsagePct(),
                sample.netRxBytesPerSec(), sample.netTxBytesPerSec());
    }

    public int[][] insertProcesses(List<ProcessSample> samples, String host) {
        if (samples.isEmpty()) {
            return new int[0][];
        }
        return jdbc.batchUpdate("""
                INSERT INTO process_metrics (collected_at, host, pid, process_name,
                    cpu_usage_pct, mem_rss_bytes, mem_usage_pct)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                samples, 200,
                (ps, sample) -> {
                    ps.setTimestamp(1, Timestamp.from(sample.collectedAt()));
                    ps.setString(2, host);
                    ps.setLong(3, sample.pid());
                    ps.setString(4, sample.processName());
                    ps.setDouble(5, sample.cpuUsagePct());
                    ps.setLong(6, sample.memRssBytes());
                    ps.setDouble(7, sample.memUsagePct());
                });
    }

    /** 系统指标按 bucketSeconds 聚合成时间序列。 */
    public List<SystemBucket> querySystemBuckets(Instant from, Instant to, int bucketSeconds) {
        return jdbc.query("""
                SELECT to_timestamp(floor(extract(epoch FROM collected_at) / ?) * ?) AS bucket,
                       avg(cpu_usage_pct), avg(mem_usage_pct), avg(disk_usage_pct),
                       avg(net_rx_bytes_per_sec), avg(net_tx_bytes_per_sec), count(*)
                FROM system_metrics
                WHERE collected_at >= ? AND collected_at < ?
                GROUP BY bucket
                ORDER BY bucket
                """,
                (rs, rowNum) -> new SystemBucket(
                        rs.getTimestamp(1).toInstant(),
                        rs.getDouble(2), rs.getDouble(3), rs.getDouble(4),
                        rs.getDouble(5), rs.getDouble(6), rs.getLong(7)),
                bucketSeconds, bucketSeconds,
                Timestamp.from(from), Timestamp.from(to));
    }

    /** 进程指标原始行（时间区间内，按时间倒序）。 */
    public List<ProcessRow> queryProcesses(Instant from, Instant to, int limit) {
        return jdbc.query("""
                SELECT collected_at, pid, process_name, cpu_usage_pct, mem_rss_bytes, mem_usage_pct
                FROM process_metrics
                WHERE collected_at >= ? AND collected_at < ?
                ORDER BY collected_at DESC, cpu_usage_pct DESC
                LIMIT ?
                """,
                (rs, rowNum) -> new ProcessRow(
                        rs.getTimestamp(1).toInstant(),
                        rs.getLong(2), rs.getString(3),
                        rs.getDouble(4), rs.getLong(5), rs.getDouble(6)),
                Timestamp.from(from), Timestamp.from(to), limit);
    }

    public SystemPointRow latestSystem() {
        List<SystemPointRow> rows = jdbc.query("""
                SELECT collected_at, host, cpu_usage_pct, mem_total_bytes, mem_used_bytes, mem_usage_pct,
                       disk_total_bytes, disk_used_bytes, disk_usage_pct,
                       net_rx_bytes_per_sec, net_tx_bytes_per_sec
                FROM system_metrics
                ORDER BY collected_at DESC
                LIMIT 1
                """,
                (rs, rowNum) -> new SystemPointRow(
                        rs.getTimestamp(1).toInstant(), rs.getString(2), rs.getDouble(3),
                        rs.getLong(4), rs.getLong(5), rs.getDouble(6),
                        rs.getLong(7), rs.getLong(8), rs.getDouble(9),
                        rs.getDouble(10), rs.getDouble(11)));
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<ProcessRow> processesAt(Instant collectedAt) {
        return jdbc.query("""
                SELECT collected_at, pid, process_name, cpu_usage_pct, mem_rss_bytes, mem_usage_pct
                FROM process_metrics
                WHERE collected_at = ?
                ORDER BY cpu_usage_pct DESC
                """,
                (rs, rowNum) -> new ProcessRow(
                        rs.getTimestamp(1).toInstant(),
                        rs.getLong(2), rs.getString(3),
                        rs.getDouble(4), rs.getLong(5), rs.getDouble(6)),
                Timestamp.from(collectedAt));
    }

    /** 删除早于保留期的数据，返回删除行数。 */
    public long deleteOlderThan(Instant threshold) {
        long processes = jdbc.update("DELETE FROM process_metrics WHERE collected_at < ?", Timestamp.from(threshold));
        long systems = jdbc.update("DELETE FROM system_metrics WHERE collected_at < ?", Timestamp.from(threshold));
        return processes + systems;
    }

    public record SystemBucket(Instant bucket, double cpuPct, double memPct, double diskPct,
                               double netRxPerSec, double netTxPerSec, long sampleCount) {
    }

    public record ProcessRow(Instant collectedAt, long pid, String name,
                             double cpuPct, long memRssBytes, double memPct) {
    }

    public record SystemPointRow(Instant collectedAt, String host, double cpuPct,
                                 long memTotalBytes, long memUsedBytes, double memPct,
                                 long diskTotalBytes, long diskUsedBytes, double diskPct,
                                 double netRxPerSec, double netTxPerSec) {
    }
}
