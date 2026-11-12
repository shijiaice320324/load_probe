package com.example.loadprobe.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class IntervalParserTest {

    @Test
    void parsesUnits() {
        assertEquals(30, IntervalParser.parseSeconds("30s"));
        assertEquals(60, IntervalParser.parseSeconds("1m"));
        assertEquals(300, IntervalParser.parseSeconds("5m"));
        assertEquals(3600, IntervalParser.parseSeconds("1h"));
        assertEquals(86400, IntervalParser.parseSeconds("1d"));
        assertEquals(45, IntervalParser.parseSeconds("45"));
    }

    @Test
    void rejectsBadInput() {
        assertThrows(IllegalArgumentException.class, () -> IntervalParser.parseSeconds(""));
        assertThrows(IllegalArgumentException.class, () -> IntervalParser.parseSeconds("0s"));
        assertThrows(IllegalArgumentException.class, () -> IntervalParser.parseSeconds("5x"));
        assertThrows(IllegalArgumentException.class, () -> IntervalParser.parseSeconds("abc"));
    }
}
