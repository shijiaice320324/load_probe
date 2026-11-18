package com.example.loadprobe.alert;

/** 通知渠道抽象：新增渠道只需实现本接口并注册为 Bean。 */
public interface AlertNotifier {

    /** 渠道标识，写入 alerts.notify_channel。 */
    String channel();

    /** 是否已配置（例如邮件未填授权码时返回 false，服务会跳过并记录日志）。 */
    default boolean isConfigured() {
        return true;
    }

    void send(AlertEvent event, long alertId) throws Exception;
}
