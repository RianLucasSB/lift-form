package com.rianlucassb.liftform.core.domain.model.enums;

import java.math.BigDecimal;

public enum Classification {
    GOOD, FAIR, POOR;

    public static Classification fromScore(BigDecimal score) {
        if (score.compareTo(new BigDecimal("0.7")) >= 0) return GOOD;
        if (score.compareTo(new BigDecimal("0.5")) >= 0) return FAIR;
        return POOR;
    }
}