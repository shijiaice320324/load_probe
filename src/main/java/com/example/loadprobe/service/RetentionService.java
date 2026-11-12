package com.example.loadprobe.service;

import com.example.loadprobe.config.LoadProbeProperties;
import com.example.loadprobe.repository.MetricsRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/** 数据保留策略：每天清理一次超过 retention-days 的数据。 */
@Service
public class RetentionService {

    private static final Logger log = LoggerFactory.getLogger(RetentionService.class);

    private final MetricsRepository repository;
    private final LoadProbeProperties properties;

    public RetentionService(MetricsRepository repository, LoadProbeProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    @Scheduled(cron = "${loadprobe.retention-cron:0 30 3 * * *}")
    public void cleanup() {
        Instant threshold = Instant.now().minus(properties.getRetentionDays(), ChronoUnit.DAYS);
        try {
            long deleted = repository.deleteOlderThan(threshold);
            log.info("保留策略清理完成: 删除 {} 行，早于 {}（保留 {} 天）",
                    deleted, threshold, properties.getRetentionDays());
        } catch (Exception e) {
            log.error("保留策略清理失败: {}", e.toString(), e);
        }
    }
}
