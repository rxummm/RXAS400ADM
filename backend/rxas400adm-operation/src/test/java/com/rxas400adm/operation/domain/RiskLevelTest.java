package com.rxas400adm.operation.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RiskLevelTest {

    @Test
    void shouldHaveCorrectOrdering() {
        assertTrue(RiskLevel.READ.ordinal() < RiskLevel.WRITE.ordinal());
        assertTrue(RiskLevel.WRITE.ordinal() < RiskLevel.DESTRUCTIVE.ordinal());
        assertTrue(RiskLevel.DESTRUCTIVE.ordinal() < RiskLevel.CRITICAL.ordinal());
        assertTrue(RiskLevel.CRITICAL.ordinal() < RiskLevel.BREAK_GLASS.ordinal());
    }

    @Test
    void shouldHaveFiveValues() {
        assertEquals(5, RiskLevel.values().length);
    }

    @Test
    void shouldParseFromName() {
        assertEquals(RiskLevel.READ, RiskLevel.valueOf("READ"));
        assertEquals(RiskLevel.BREAK_GLASS, RiskLevel.valueOf("BREAK_GLASS"));
    }
}