package com.example.loadprobe.collector;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.software.os.OSFileStore;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

/**
 * 基于 OSHI 的指标采集器。
 * <p>系统级：CPU 使用率、内存、磁盘（所有文件系统汇总）、网络吞吐（两次采样间均值）。
 * <p>进程级：按 CPU 占用排序取 Top-N，CPU 通过两次采样的 ticks 差值计算。
 */
@Component
public class MetricsCollector {

    private static final Logger log = LoggerFactory.getLogger(MetricsCollector.class);

    private final SystemInfo systemInfo = new SystemInfo();
    private final HardwareAbstractionLayer hardware = systemInfo.getHardware();
    private final OperatingSystem operatingSystem = systemInfo.getOperatingSystem();

    private long previousNetRx = -1L;
    private long previousNetTx = -1L;
    private long previousNetAtMillis = 0L;
    private long[] previousCpuTicks = hardware.getProcessor().getSystemCpuLoadTicks();
    private Map<Integer, OSProcess> previousProcesses = new HashMap<>();

    /** 采集一次整机指标。 */
    public SystemSample collectSystem(Instant collectedAt) {
        Instant now = collectedAt;

        double cpuLoad = hardware.getProcessor().getSystemCpuLoadBetweenTicks(previousCpuTicks);
        previousCpuTicks = hardware.getProcessor().getSystemCpuLoadTicks();
        double cpuPct = cpuLoad < 0 ? 0.0 : round(cpuLoad * 100.0, 2);

        GlobalMemory memory = hardware.getMemory();
        long memTotal = memory.getTotal();
        long memUsed = memTotal - memory.getAvailable();
        double memPct = memTotal <= 0 ? 0.0 : round(memUsed * 100.0 / memTotal, 2);

        long diskTotal = 0L;
        long diskUsable = 0L;
        for (OSFileStore store : operatingSystem.getFileSystem().getFileStores()) {
            long total = store.getTotalSpace();
            if (total <= 0) {
                continue;
            }
            diskTotal += total;
            diskUsable += store.getUsableSpace();
        }
        long diskUsed = diskTotal - diskUsable;
        double diskPct = diskTotal <= 0 ? 0.0 : round(diskUsed * 100.0 / diskTotal, 2);

        long netRx = 0L;
        long netTx = 0L;
        for (NetworkIF networkIF : hardware.getNetworkIFs()) {
            netRx += networkIF.getBytesRecv();
            netTx += networkIF.getBytesSent();
        }
        double rxPerSec = 0.0;
        double txPerSec = 0.0;
        if (previousNetRx >= 0 && previousNetAtMillis > 0) {
            long elapsedMillis = Math.max(1L, now.toEpochMilli() - previousNetAtMillis);
            rxPerSec = round(Math.max(0L, netRx - previousNetRx) * 1000.0 / elapsedMillis, 2);
            txPerSec = round(Math.max(0L, netTx - previousNetTx) * 1000.0 / elapsedMillis, 2);
        }
        previousNetRx = netRx;
        previousNetTx = netTx;
        previousNetAtMillis = now.toEpochMilli();

        return new SystemSample(now, cpuPct, memTotal, memUsed, memPct,
                diskTotal, diskUsed, diskPct, rxPerSec, txPerSec);
    }

    /** 采集 CPU 占用最高的前 N 个进程（归一化为整机占比，排除 Idle 进程）。 */
    public List<ProcessSample> collectTopProcesses(Instant collectedAt, int topN) {
        Instant now = collectedAt;
        long memTotal = hardware.getMemory().getTotal();
        int cpuCount = Math.max(1, hardware.getProcessor().getLogicalProcessorCount());

        List<OSProcess> processes = operatingSystem.getProcesses(
                OperatingSystem.ProcessFiltering.ALL_PROCESSES, OperatingSystem.ProcessSorting.NO_SORTING, 0);
        List<ProcessSample> samples = new ArrayList<>(processes.size());
        Map<Integer, OSProcess> currentSnapshot = new HashMap<>(processes.size() * 2);

        for (OSProcess process : processes) {
            int pid = process.getProcessID();
            // PID 0 是 Windows 的 Idle 进程，CPU 数值无意义，直接跳过
            if (pid == 0) {
                continue;
            }
            currentSnapshot.put(pid, process);

            OSProcess previous = previousProcesses.get(pid);
            double cpu = previous == null
                    ? process.getProcessCpuLoadCumulative()
                    : process.getProcessCpuLoadBetweenTicks(previous);
            if (cpu < 0) {
                cpu = 0.0;
            }
            long rss = Math.max(0L, process.getResidentSetSize());
            double memPct = memTotal <= 0 ? 0.0 : round(rss * 100.0 / memTotal, 2);

            samples.add(new ProcessSample(now, pid, safeName(process),
                    round(cpu * 100.0 / cpuCount, 2), rss, memPct));
        }
        previousProcesses = currentSnapshot;

        samples.sort(Comparator.comparingDouble(ProcessSample::cpuUsagePct).reversed());
        if (samples.size() > topN) {
            return new ArrayList<>(samples.subList(0, topN));
        }
        log.debug("采集进程数={} 返回 Top={}", samples.size(), topN);
        return samples;
    }

    private static String safeName(OSProcess process) {
        String name = process.getName();
        if (name == null || name.isBlank()) {
            return "unknown";
        }
        return name.length() > 120 ? name.substring(0, 120) : name;
    }

    private static double round(double value, int scale) {
        double factor = Math.pow(10, scale);
        return Math.round(value * factor) / factor;
    }
}
