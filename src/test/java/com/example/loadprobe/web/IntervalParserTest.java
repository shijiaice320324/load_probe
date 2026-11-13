package com.example.loadprobe.web;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("IntervalParser 查询间隔解析")
class IntervalParserTest {

    @Test
    @DisplayName("A1-1 解析各时间单位")
    void parsesAllUnits() {
        assertAll(
                () -> assertEquals(30, IntervalParser.parseSeconds("30s")),
                () -> assertEquals(60, IntervalParser.parseSeconds("1m")),
                () -> assertEquals(300, IntervalParser.parseSeconds("5m")),
                () -> assertEquals(3600, IntervalParser.parseSeconds("1h")),
                () -> assertEquals(86400, IntervalParser.parseSeconds("1d")));
    }

    @Test
    @DisplayName("A1-2 纯数字按秒解析")
    void parsesPlainSeconds() {
        assertEquals(45, IntervalParser.parseSeconds("45"));
        assertEquals(1, IntervalParser.parseSeconds("1"));
    }

    @Test
    @DisplayName("A1-3 忽略首尾空格与大小写")
    void trimsAndIgnoresCase() {
        assertAll(
                () -> assertEquals(300, IntervalParser.parseSeconds(" 5M ")),
                () -> assertEquals(3600, IntervalParser.parseSeconds("1H")),
                () -> assertEquals(30, IntervalParser.parseSeconds("\t30S\n")));
    }

    @Test
    @DisplayName("A1-4 空值或空白字符串报错")
    void rejectsBlank() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> IntervalParser.parseSeconds(null)),
                () -> assertThrows(IllegalArgumentException.class, () -> IntervalParser.parseSeconds("")),
                () -> assertThrows(IllegalArgumentException.class, () -> IntervalParser.parseSeconds("   ")));
    }

    @Test
    @DisplayName("A1-5 非法单位或非数字报错")
    void rejectsUnsupportedUnit() {
        IllegalArgumentException unsupported =
                assertThrows(IllegalArgumentException.class, () -> IntervalParser.parseSeconds("5x"));
        assertTrue(unsupported.getMessage().contains("不支持"), "提示应说明单位不受支持");

        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> IntervalParser.parseSeconds("abc")),
                () -> assertThrows(IllegalArgumentException.class, () -> IntervalParser.parseSeconds("1w")),
                () -> assertThrows(IllegalArgumentException.class, () -> IntervalParser.parseSeconds("m")));
    }

    @Test
    @DisplayName("A1-6 零或负数报错")
    void rejectsNonPositive() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> IntervalParser.parseSeconds("0")),
                () -> assertThrows(IllegalArgumentException.class, () -> IntervalParser.parseSeconds("0s")),
                () -> assertThrows(IllegalArgumentException.class, () -> IntervalParser.parseSeconds("-5")),
                () -> assertThrows(IllegalArgumentException.class, () -> IntervalParser.parseSeconds("-5s")));
    }

    @Test
    @DisplayName("A1-8 超出 int 范围报错且不溢出")
    void rejectsOverflow() {
        // 50000 天 ≈ 4.32e9 秒，超过 int 上限；修复前会截断成 25032704 秒（约 290 天）并通过校验
        IllegalArgumentException silentTruncation =
                assertThrows(IllegalArgumentException.class, () -> IntervalParser.parseSeconds("50000d"));
        assertTrue(silentTruncation.getMessage().contains("过大"), "提示应说明数值过大");

        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> IntervalParser.parseSeconds("999999999d")),
                () -> assertThrows(IllegalArgumentException.class, () -> IntervalParser.parseSeconds("2147483648")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> IntervalParser.parseSeconds("99999999999999999999")));
    }

    @Test
    @DisplayName("A1-9 边界值可用")
    void acceptsBoundaries() {
        assertAll(
                () -> assertEquals(1, IntervalParser.parseSeconds("1s")),
                () -> assertEquals(Integer.MAX_VALUE, IntervalParser.parseSeconds(String.valueOf(Integer.MAX_VALUE))));
    }
}
