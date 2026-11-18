package com.example.loadprobe.alert;

import com.example.loadprobe.collector.CollectionStatus;
import com.example.loadprobe.collector.SystemSample;
import com.example.loadprobe.config.LoadProbeProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 告警编排：判定 → 落库 → 异步通知。
 * 通知失败只记录，不影响采集与查询。
 */
@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final AlertProperties properties;
    private final AlertEvaluator evaluator;
    private final AlertRepository repository;
    private final List<AlertNotifier> notifiers;
    private final LoadProbeProperties loadProbeProperties;
    private final CollectionStatus collectionStatus;
    private final ExecutorService notifierPool = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "alert-notifier");
        thread.setDaemon(true);
        return thread;
    });

    public AlertService(AlertProperties properties,
                        AlertEvaluator evaluator,
                        AlertRepository repository,
                        List<AlertNotifier> notifiers,
                        LoadProbeProperties loadProbeProperties,
                        CollectionStatus collectionStatus) {
        this.properties = properties;
        this.evaluator = evaluator;
        this.repository = repository;
        this.notifiers = notifiers;
        this.loadProbeProperties = loadProbeProperties;
        this.collectionStatus = collectionStatus;
    }

    /** 启动时清理上次遗留的“触发中”记录（内存状态在重启后已丢失）。 */
    @EventListener(ApplicationReadyEvent.class)
    public void closeStaleOpenAlerts() {
        try {
            int closed = repository.resolveAllFiring(Instant.now());
            if (closed > 0) {
                log.warn("服务启动：自动关闭 {} 条上次未恢复的告警记录（状态可能已变化，请查看历史）", closed);
            }
        } catch (Exception e) {
            log.error("启动时清理未恢复告警失败: {}", e.toString(), e);
        }
    }

    /** 每次采集成功后调用。 */
    public void onCollection(SystemSample sample, Instant collectedAt) {        if (!properties.isEnabled()) {
            return;
        }
        try {
            List<AlertEvent> events = evaluator.evaluate(
                    MetricsSnapshot.ofCollection(sample.cpuUsagePct(), sample.memUsagePct(), sample.diskUsagePct()),
                    collectedAt);
            dispatch(events);
        } catch (Exception e) {
            log.error("告警判定失败: {}", e.toString(), e);
        }
    }

    /** 每 30 秒检查一次是否采集断档（覆盖数据库故障、采集线程卡死等情况）。 */
    @Scheduled(fixedDelayString = "30000", initialDelayString = "30000")
    public void checkCollectStaleness() {
        if (!properties.isEnabled()) {
            return;
        }
        Instant last = collectionStatus.getLastCollectedAt();
        if (last == null) {
            return;
        }
        try {
            long ageSeconds = Duration.between(last, Instant.now()).getSeconds();
            dispatch(evaluator.evaluate(MetricsSnapshot.ofCollectAge(ageSeconds), Instant.now()));
        } catch (Exception e) {
            log.error("断档检测失败: {}", e.toString(), e);
        }
    }

    private void dispatch(List<AlertEvent> events) {
        for (AlertEvent event : events) {
            if (event.status() == AlertStatus.RESOLVED) {
                repository.resolve(event.rule().getId(), event.at());
                notifyAsync(event, -1);
                continue;
            }
            long alertId = repository.insert(event, loadProbeProperties.getHost());
            if (event.notifyEnabled()) {
                notifyAsync(event, alertId);
            } else {
                log.info("告警处于冷却期，仅记录不通知 rule={} alertId={}", event.rule().getId(), alertId);
            }
        }
    }

    private void notifyAsync(AlertEvent event, long alertId) {
        notifierPool.submit(() -> {
            for (AlertNotifier notifier : notifiers) {
                if (!notifier.isConfigured()) {
                    log.warn("通知渠道 {} 未配置，跳过（alertId={}）", notifier.channel(), alertId);
                    continue;
                }
                try {
                    notifier.send(event, alertId);
                    if (alertId > 0) {
                        repository.markNotified(alertId, notifier.channel());
                    }
                } catch (Exception e) {
                    log.error("告警通知失败 channel={} alertId={}: {}", notifier.channel(), alertId, e.toString());
                    if (alertId > 0) {
                        repository.markNotifyFailed(alertId, e.toString());
                    }
                }
            }
        });
    }

    /** 供测试等待异步通知完成。 */
    void awaitNotifications(long timeoutMillis) throws InterruptedException {
        notifierPool.shutdown();
        notifierPool.awaitTermination(timeoutMillis, TimeUnit.MILLISECONDS);
    }
}
