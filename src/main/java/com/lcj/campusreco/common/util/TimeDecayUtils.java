package com.lcj.campusreco.common.util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Duration;
import java.time.LocalDateTime;

public final class TimeDecayUtils {

    private static final MathContext MATH_CONTEXT = MathContext.DECIMAL64;

    private TimeDecayUtils() {
    }

    public static BigDecimal calculateDecay(LocalDateTime sourceTime, LocalDateTime referenceTime, BigDecimal decayFactor) {
        if (sourceTime == null || referenceTime == null || decayFactor == null) {
            return BigDecimal.ONE;
        }
        long days = Math.max(0, Duration.between(sourceTime, referenceTime).toDays());
        double factor = Math.exp(decayFactor.negate().multiply(BigDecimal.valueOf(days), MATH_CONTEXT).doubleValue());
        return BigDecimal.valueOf(factor);
    }
}
