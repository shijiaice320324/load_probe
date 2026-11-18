package com.example.loadprobe.alert;

/** 可监控的指标类型。 */
public enum AlertMetric {
    /** 整机 CPU 使用率（%） */
    CPU,
    /** 内存使用率（%） */
    MEM,
    /** 磁盘使用率（%） */
    DISK,
    /** 距上次成功采集的秒数，用于断档检测 */
    COLLECT_AGE_SECONDS
}
