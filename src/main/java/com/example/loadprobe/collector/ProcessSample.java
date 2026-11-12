package com.example.loadprobe.collector;

import java.time.Instant;

/** 一次进程级采样（Top-N 中的某一行）。 */
public record ProcessSample(
        Instant collectedAt,
        long pid,
        String processName,
        double cpuUsagePct,
        long memRssBytes,
        double memUsagePct) {
}
