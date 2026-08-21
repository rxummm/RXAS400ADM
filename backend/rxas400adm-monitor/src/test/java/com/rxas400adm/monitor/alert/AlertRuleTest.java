package com.rxas400adm.monitor.alert;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlertRuleTest {

    @Test
    void cpuOverThreshold_shouldMatch() {
        AlertRule rule = new AlertRule();
        rule.setMetricName("CPU");
        rule.setOperator(">");
        rule.setThreshold(90.0);
        rule.setEnabled(true);

        assertTrue(rule.match(95.0));
        assertFalse(rule.match(80.0));
    }

    @Test
    void disabledRule_shouldNeverMatch() {
        AlertRule rule = new AlertRule();
        rule.setOperator(">");
        rule.setThreshold(1.0);
        rule.setEnabled(false);

        assertFalse(rule.match(99.0));
    }

    @Test
    void greaterThanOrEqual_shouldMatchBoundary() {
        AlertRule rule = new AlertRule();
        rule.setOperator(">=");
        rule.setThreshold(85.0);
        rule.setEnabled(true);

        assertTrue(rule.match(85.0));
        assertFalse(rule.match(84.9));
    }

    @Test
    void lessThan_shouldMatch() {
        AlertRule rule = new AlertRule();
        rule.setOperator("<");
        rule.setThreshold(10.0);
        rule.setEnabled(true);

        assertTrue(rule.match(5.0));
        assertFalse(rule.match(15.0));
    }
}
