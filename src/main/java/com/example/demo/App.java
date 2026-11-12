package com.example.demo;

/**
 * 起步示例程序。
 */
public final class App {

    private App() {
    }

    public static void main(String[] args) {
        System.out.println(greeting("Java 21"));
    }

    /**
     * 生成问候语，便于单元测试覆盖。
     *
     * @param name 问候对象，null 或空白时回退为 "World"
     * @return 问候语
     */
    static String greeting(String name) {
        String who = (name == null || name.isBlank()) ? "World" : name.trim();
        return "Hello, " + who + "!";
    }
}
