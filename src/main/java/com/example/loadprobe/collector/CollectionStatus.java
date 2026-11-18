package com.example.loadprobe.collector;

import java.time.Instant;
import org.springframework.stereotype.Component;

/** 最近一次成功采集的时间，供断档检测使用（避免与告警服务循环依赖）。 */
@Component
public class CollectionStatus {

    private volatile Instant lastCollectedAt;

    public void markCollected(Instant at) {
        this.lastCollectedAt = at;
    }

    public Instant getLastCollectedAt() {
        return lastCollectedAt;
    }
}
