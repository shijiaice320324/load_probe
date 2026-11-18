package com.example.loadprobe.alert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** 兜底渠道：始终把告警写进日志，保证不丢事件。 */
@Component
public class LoggingAlertNotifier implements AlertNotifier {

    private static final Logger log = LoggerFactory.getLogger(LoggingAlertNotifier.class);

    @Override
    public String channel() {
        return "log";
    }

    @Override
    public void send(AlertEvent event, long alertId) {
        log.info("[ALERT#{}] {} {} {} {}={} threshold={} at={}",
                alertId, event.status(), event.level(), event.rule().name(),
                event.metric(), event.value(), event.threshold(), event.at());
    }
}
