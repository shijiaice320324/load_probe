package com.example.loadprobe.config;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 读取 HikariCP 连接池实时状态，供 /health 暴露。
 * 不依赖 JMX：HikariDataSource 直接提供 MXBean。
 */
@Component
public class DbPoolInfo {

    private static final Logger log = LoggerFactory.getLogger(DbPoolInfo.class);
    private final HikariDataSource dataSource;

    public DbPoolInfo(DataSource dataSource) {
        if (dataSource instanceof HikariDataSource hikari) {
            this.dataSource = hikari;
        } else {
            this.dataSource = null;
            log.warn("当前数据源不是 HikariDataSource（{}），连接池指标不可用", dataSource.getClass().getName());
        }
    }

    /** 连接池快照；读取失败时返回全 -1，避免影响健康检查。 */
    public Snapshot snapshot() {
        if (dataSource == null) {
            return new Snapshot(-1, -1, -1, -1, -1);
        }
        try {
            HikariPoolMXBean pool = dataSource.getHikariPoolMXBean();
            if (pool == null) {
                return new Snapshot(-1, -1, -1, -1, dataSource.getMaximumPoolSize());
            }
            return new Snapshot(
                    pool.getActiveConnections(),
                    pool.getIdleConnections(),
                    pool.getTotalConnections(),
                    pool.getThreadsAwaitingConnection(),
                    dataSource.getMaximumPoolSize());
        } catch (Exception e) {
            log.warn("读取连接池状态失败: {}", e.toString());
            return new Snapshot(-1, -1, -1, -1, -1);
        }
    }

    /**
     * @param active        正在被业务使用的连接数
     * @param idle          空闲连接数
     * @param total         active + idle
     * @param awaiting      正在等待连接的线程数（&gt;0 说明池子成了瓶颈）
     * @param maxPoolSize   池上限
     */
    public record Snapshot(int active, int idle, int total, int awaiting, int maxPoolSize) {
    }
}
