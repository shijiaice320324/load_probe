package com.example.loadprobe.alert;

import java.util.List;

/** 运行时规则来源，便于替换（数据库实现 / 测试桩）。 */
public interface AlertRuleProvider {
    List<AlertRuleDef> rules();
}
