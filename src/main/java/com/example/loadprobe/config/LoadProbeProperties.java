package com.example.loadprobe.config;

import jakarta.validation.constraints.Min;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 采集与查询相关的可配置项，前缀 loadprobe。
 * 采集间隔最小 10 秒（需求约束），启动时由 Spring 校验。
 */
@Validated
@ConfigurationProperties(prefix = "loadprobe")
public class LoadProbeProperties {

    /** 采集间隔（秒），默认 60，最快 10。 */
    @Min(value = 10, message = "loadprobe.interval-seconds 最小为 10 秒")
    private int intervalSeconds = 60;

    /** 每轮采集 CPU 占用最高的前 N 个进程。 */
    @Min(value = 1, message = "loadprobe.top-process-count 至少为 1")
    private int topProcessCount = 10;

    /** 数据保留天数，默认 7 天。 */
    @Min(value = 1, message = "loadprobe.retention-days 至少为 1")
    private int retentionDays = 7;

    /** 清理任务 cron。 */
    private String retentionCron = "0 30 3 * * *";

    /** 查询接口密钥；为空表示不校验（仅本机调试用）。 */
    private String apiKey = "";

    /** 单次查询最多返回的聚合点数量。 */
    @Min(value = 1, message = "loadprobe.max-points 至少为 1")
    private int maxPoints = 2000;

    /**
     * 免鉴权路径（前缀匹配）。
     * 默认放行健康检查，方便浏览器/探针直接访问；查询接口仍然需要 X-API-Key。
     * 配置为空列表即恢复“所有 /api 都要鉴权”。
     */
    private List<String> authExemptPaths = new ArrayList<>(List.of("/api/v1/health"));

    /** 写入数据库的机器标识。 */
    private String host = "localhost";

    public int getIntervalSeconds() {
        return intervalSeconds;
    }

    public void setIntervalSeconds(int intervalSeconds) {
        this.intervalSeconds = intervalSeconds;
    }

    public int getTopProcessCount() {
        return topProcessCount;
    }

    public void setTopProcessCount(int topProcessCount) {
        this.topProcessCount = topProcessCount;
    }

    public int getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(int retentionDays) {
        this.retentionDays = retentionDays;
    }

    public String getRetentionCron() {
        return retentionCron;
    }

    public void setRetentionCron(String retentionCron) {
        this.retentionCron = retentionCron;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public int getMaxPoints() {
        return maxPoints;
    }

    public void setMaxPoints(int maxPoints) {
        this.maxPoints = maxPoints;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public List<String> getAuthExemptPaths() {
        return authExemptPaths;
    }

    public void setAuthExemptPaths(List<String> authExemptPaths) {
        this.authExemptPaths = authExemptPaths;
    }
}
