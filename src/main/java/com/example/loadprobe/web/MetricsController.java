package com.example.loadprobe.web;

import com.example.loadprobe.config.DbPoolInfo;
import com.example.loadprobe.config.LoadProbeProperties;
import com.example.loadprobe.config.TlsInfo;
import com.example.loadprobe.repository.MetricsRepository;
import com.example.loadprobe.repository.MetricsRepository.ProcessRow;
import com.example.loadprobe.repository.MetricsRepository.SystemBucket;
import com.example.loadprobe.repository.MetricsRepository.SystemPointRow;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 指标查询接口，全部需要 X-API-Key（见 ApiKeyFilter）。 */
@RestController
@RequestMapping("/api/v1")
public class MetricsController {

    private static final Logger log = LoggerFactory.getLogger(MetricsController.class);
    /** 进程查询单次最多返回的行数，防止一次拉出过大的 JSON。 */
    static final int MAX_PROCESS_LIMIT = 1000;
    private final MetricsRepository repository;
    private final LoadProbeProperties properties;
    private final TlsInfo tlsInfo;
    private final DbPoolInfo dbPoolInfo;

    public MetricsController(MetricsRepository repository, LoadProbeProperties properties,
                             TlsInfo tlsInfo, DbPoolInfo dbPoolInfo) {
        this.repository = repository;
        this.properties = properties;
        this.tlsInfo = tlsInfo;
        this.dbPoolInfo = dbPoolInfo;
    }

    /** 系统级指标时间序列，支持任意时间跨度并按 interval 聚合。 */
    @GetMapping("/metrics/system")
    public SystemSeries metricsSystem(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String interval) {
        Window window = resolveWindow(from, to);
        int bucketSeconds = resolveBucketSeconds(window, interval);
        List<SystemBucket> buckets = repository.querySystemBuckets(window.from(), window.to(), bucketSeconds);
        log.debug("查询系统指标 from={} to={} bucket={}s rows={}", window.from(), window.to(), bucketSeconds, buckets.size());
        return new SystemSeries(window.from(), window.to(), bucketSeconds, properties.getHost(),
                buckets.stream()
                        .map(b -> new SystemPoint(b.bucket(), b.cpuPct(), b.memPct(), b.diskPct(),
                                b.netRxPerSec(), b.netTxPerSec(), b.sampleCount()))
                        .toList());
    }

    /** 进程级指标（原始行），按时间倒序，limit 控制返回条数。 */
    @GetMapping("/metrics/processes")
    public ProcessSeries metricsProcesses(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false, defaultValue = "200") int limit) {
        // 上限收紧到 1000：单行约 127 字节，1000 行约 120KB，避免一次拉出几 MB 的 JSON
        if (limit < 1 || limit > MAX_PROCESS_LIMIT) {
            throw new IllegalArgumentException("limit 取值范围 1.." + MAX_PROCESS_LIMIT);
        }
        Window window = resolveWindow(from, to);
        List<ProcessRow> rows = repository.queryProcesses(window.from(), window.to(), limit);
        if (rows.size() >= limit) {
            log.debug("进程查询命中 limit 上限 {}，如需更早数据请缩小时间窗或调小 limit", limit);
        }
        log.debug("查询进程指标 from={} to={} limit={} rows={}", window.from(), window.to(), limit, rows.size());
        return new ProcessSeries(window.from(), window.to(), limit, rows.stream().map(ProcessSeries::toPoint).toList());
    }

    /** 最近一次采集结果（系统 + 该时刻的 Top 进程）。 */
    @GetMapping("/metrics/latest")
    public Latest latest() {
        SystemPointRow system = repository.latestSystem();
        if (system == null) {
            return new Latest(null, List.of());
        }
        List<ProcessPoint> processes = repository.processesAt(system.collectedAt()).stream()
                .map(ProcessSeries::toPoint)
                .toList();
        return new Latest(
                new LatestSystem(system.collectedAt(), system.host(), system.cpuPct(),
                        system.memTotalBytes(), system.memUsedBytes(), system.memPct(),
                        system.diskTotalBytes(), system.diskUsedBytes(), system.diskPct(),
                        system.netRxPerSec(), system.netTxPerSec()),
                processes);
    }

    /** 健康检查 + 当前配置摘要（含 TLS 证书与连接池状态）。 */
    @GetMapping("/health")
    public Health health() {
        DbPoolInfo.Snapshot pool = dbPoolInfo.snapshot();
        return new Health("UP", properties.getHost(), properties.getIntervalSeconds(),
                properties.getTopProcessCount(), properties.getRetentionDays(), Instant.now(),
                new TlsView(tlsInfo.isEnabled(), tlsInfo.getSubject(), tlsInfo.getNotAfter(),
                        tlsInfo.getDaysRemaining()),
                new DbPoolView(pool.active(), pool.idle(), pool.total(), pool.awaiting(), pool.maxPoolSize()));
    }

    private Window resolveWindow(String fromRaw, String toRaw) {
        Instant to = toRaw == null || toRaw.isBlank() ? Instant.now() : parseInstant(toRaw, "to");
        Instant from = fromRaw == null || fromRaw.isBlank()
                ? to.minus(Duration.ofHours(1))
                : parseInstant(fromRaw, "from");
        if (!from.isBefore(to)) {
            throw new IllegalArgumentException("from 必须早于 to");
        }
        return new Window(from, to);
    }

    private int resolveBucketSeconds(Window window, String intervalRaw) {
        long spanSeconds = Math.max(1, Duration.between(window.from(), window.to()).getSeconds());
        int bucket = intervalRaw == null || intervalRaw.isBlank()
                ? properties.getIntervalSeconds()
                : IntervalParser.parseSeconds(intervalRaw);
        // 点太多时自动放大桶，避免一次返回过多数据
        long estimated = spanSeconds / Math.max(1, bucket);
        if (estimated > properties.getMaxPoints()) {
            bucket = (int) Math.min(Integer.MAX_VALUE, Math.max(1, spanSeconds / properties.getMaxPoints()));
        }
        return bucket;
    }

    private static Instant parseInstant(String raw, String name) {
        try {
            return Instant.parse(raw);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(name + " 需为 ISO-8601 时间，例如 2026-11-12T00:00:00Z，收到: " + raw);
        }
    }

    private record Window(Instant from, Instant to) {
    }

    public record SystemPoint(Instant ts, double cpuUsagePct, double memUsagePct, double diskUsagePct,
                              double netRxBytesPerSec, double netTxBytesPerSec, long sampleCount) {
    }

    public record SystemSeries(Instant from, Instant to, int intervalSeconds, String host,
                               List<SystemPoint> points) {
    }

    public record ProcessPoint(Instant ts, long pid, String name, double cpuUsagePct,
                               long memRssBytes, double memUsagePct) {
        static ProcessPoint from(ProcessRow row) {
            return new ProcessPoint(row.collectedAt(), row.pid(), row.name(),
                    row.cpuPct(), row.memRssBytes(), row.memPct());
        }
    }

    public record ProcessSeries(Instant from, Instant to, int limit, List<ProcessPoint> points) {
        static ProcessPoint toPoint(ProcessRow row) {
            return ProcessPoint.from(row);
        }
    }

    public record LatestSystem(Instant ts, String host, double cpuUsagePct, long memTotalBytes,
                               long memUsedBytes, double memUsagePct, long diskTotalBytes,
                               long diskUsedBytes, double diskUsagePct, double netRxBytesPerSec,
                               double netTxBytesPerSec) {
    }

    public record Latest(LatestSystem system, List<ProcessPoint> processes) {
    }

    public record Health(String status, String host, int intervalSeconds, int topProcessCount,
                         int retentionDays, Instant now, TlsView tls, DbPoolView dbPool) {
    }

    /** TLS 证书信息，便于外部监控到期时间。 */
    public record TlsView(boolean enabled, String subject, Instant notAfter, long daysRemaining) {
    }

    /**
     * 连接池状态。
     *
     * @param active         正在使用的连接
     * @param idle           空闲连接
     * @param total          active + idle
     * @param awaitingThreads 等待连接的线程数，持续 &gt; 0 说明池子偏小
     * @param maxPoolSize    池上限
     */
    public record DbPoolView(int active, int idle, int total, int awaitingThreads, int maxPoolSize) {
    }
}
