package com.example.loadprobe.collector;

import java.time.Instant;

/** 一次系统级采样（整机 CPU / 内存 / 磁盘 / 网络）。 */
public record SystemSample(
        Instant collectedAt,
        double cpuUsagePct,
        long memTotalBytes,
        long memUsedBytes,
        double memUsagePct,
        long diskTotalBytes,
        long diskUsedBytes,
        double diskUsagePct,
        double netRxBytesPerSec,
        double netTxBytesPerSec) {
}
