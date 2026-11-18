package com.example.loadprobe.alert;

import java.time.Instant;

/**
 * 一次状态变化事件：FIRING = 刚触发，RESOLVED = 刚恢复。
 * notifyEnabled=false 表示处于冷却期，只落库不重复通知。
 * 注意：记录组件不能叫 notify（与 Object.notify() 冲突）。
 */
public record AlertEvent(
        AlertRuleDef rule,
        AlertStatus status,
        AlertLevel level,
        AlertMetric metric,
        double value,
        double threshold,
        Instant at,
        boolean notifyEnabled,
        String message) {
}
