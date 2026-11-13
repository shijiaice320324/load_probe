package com.example.loadprobe.web;

import java.util.Locale;

/** 解析查询接口的 interval 参数：支持 30s / 1m / 5m / 1h / 1d，或纯数字表示秒。 */
public final class IntervalParser {

    private IntervalParser() {
    }

    public static int parseSeconds(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("interval 不能为空");
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        char unit = value.charAt(value.length() - 1);
        if (Character.isDigit(unit)) {
            return validate(Long.parseLong(value), raw);
        }
        long amount = Long.parseLong(value.substring(0, value.length() - 1));
        long seconds = switch (unit) {
            case 's' -> amount;
            case 'm' -> amount * 60;
            case 'h' -> amount * 3600;
            case 'd' -> amount * 86400;
            default -> throw new IllegalArgumentException("不支持的 interval 单位: " + raw + "（可用 s/m/h/d）");
        };
        return validate(seconds, raw);
    }

    /**
     * 校验并转换。注意必须在 long 上判断上限，不能先 (int) 强转再校验——
     * 那样 999999999d 这类大值会溢出成看似合法的秒数。
     */
    private static int validate(long seconds, String raw) {
        if (seconds < 1) {
            throw new IllegalArgumentException("interval 至少为 1 秒");
        }
        if (seconds > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("interval 过大: " + raw + "（最大 " + Integer.MAX_VALUE + " 秒）");
        }
        return (int) seconds;
    }
}
