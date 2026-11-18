package com.example.loadprobe.alert;

/** 一次判定所看到的指标快照；不需要的指标填 Double.NaN。 */
public record MetricsSnapshot(double cpu, double mem, double disk, double collectAgeSeconds) {

    public static MetricsSnapshot ofCollection(double cpu, double mem, double disk) {
        return new MetricsSnapshot(cpu, mem, disk, Double.NaN);
    }

    public static MetricsSnapshot ofCollectAge(double ageSeconds) {
        return new MetricsSnapshot(Double.NaN, Double.NaN, Double.NaN, ageSeconds);
    }

    public double valueOf(AlertMetric metric) {
        return switch (metric) {
            case CPU -> cpu;
            case MEM -> mem;
            case DISK -> disk;
            case COLLECT_AGE_SECONDS -> collectAgeSeconds;
        };
    }
}
