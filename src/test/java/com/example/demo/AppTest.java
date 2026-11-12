package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AppTest {

    @Test
    void greetingWithName() {
        assertEquals("Hello, Java 21!", App.greeting("Java 21"));
    }

    @Test
    void greetingFallsBackToWorld() {
        assertEquals("Hello, World!", App.greeting(null));
        assertEquals("Hello, World!", App.greeting("   "));
    }

    @Test
    void greetingTrimsInput() {
        assertEquals("Hello, DSH!", App.greeting("  DSH  "));
    }
}
