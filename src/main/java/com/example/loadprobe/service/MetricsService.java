package com.example.loadprobe.service;

import com.example.loadprobe.collector.MetricsCollector;
import com.example.loadprobe.collector.ProcessSample;
import com.example.loadprobe.collector.SystemSample;
import com.example.loadprobe.config.LoadProbeProperties;
import com.example.loadprobe.repository.MetricsRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/** 按固定间隔采集并落库。间隔来自 loadprobe.interval-seconds（默认 60，最小 10）。 */
@Service
public class MetricsService {

    private static final Logger log = LoggerFactory.getLogger(MetricsService.class);

    private final MetricsCollector collector;
    private final MetricsRepository repository;
    private final LoadProbeProperties properties;

    public MetricsService(MetricsCollector collector, MetricsRepository repository, LoadProbeProperties properties) {
        this.collector = collector;
        this.repository = repository;
        this.properties = properties;
    }

    @Scheduled(initialDelayString = "5000", fixedDelayString = "#{${loadprobe.interval-seconds} * 1000}")
    public void scheduledCollect() {
        collectOnce();
    }

    /** 执行一次采集（失败只记录日志，不影响下一轮）。 */
    public void collectOnce() {
        long startedAt = System.currentTimeMillis();
        try {
            Instant collectedAt = Instant.now();
            SystemSample system = collector.collectSystem(collectedAt);
            repository.insertSystem(system, properties.getHost());

            List<ProcessSample> processes = collector.collectTopProcesses(collectedAt, properties.getTopProcessCount());
            repository.insertProcesses(processes, properties.getHost());

            log.info("采集完成 host={} cpu={}% mem={}% ({}MB/{}MB) disk={}% netIn={}B/s netOut={}B/s topProcesses={} 耗时={}ms",
                    properties.getHost(), system.cpuUsagePct(), system.memUsagePct(),
                    system.memUsedBytes() / 1024 / 1024, system.memTotalBytes() / 1024 / 1024,
                    system.diskUsagePct(), system.netRxBytesPerSec(), system.netTxBytesPerSec(),
                    processes.size(), System.currentTimeMillis() - startedAt);
            if (log.isDebugEnabled() && !processes.isEmpty()) {
                processes.stream().limit(3).forEach(p -> log.debug("  process pid={} name={} cpu={}% mem={}MB",
                        p.pid(), p.processName(), p.cpuUsagePct(), p.memRssBytes() / 1024 / 1024));
            }
        } catch (Exception e) {
            log.error("采集失败，将在下一轮重试: {}", e.toString(), e);
        }
    }
}
