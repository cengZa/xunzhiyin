package com.lcj.campusreco.common.util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Map;

public final class VectorUtils {

    private static final MathContext MATH_CONTEXT = MathContext.DECIMAL64;

    private VectorUtils() {
    }

    public static BigDecimal cosineSimilarity(Map<Long, BigDecimal> left, Map<Long, BigDecimal> right) {
        if (left == null || right == null || left.isEmpty() || right.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal dot = BigDecimal.ZERO;
        BigDecimal leftNorm = BigDecimal.ZERO;
        BigDecimal rightNorm = BigDecimal.ZERO;
        for (Map.Entry<Long, BigDecimal> entry : left.entrySet()) {
            BigDecimal leftValue = safe(entry.getValue());
            BigDecimal rightValue = safe(right.get(entry.getKey()));
            dot = dot.add(leftValue.multiply(rightValue, MATH_CONTEXT), MATH_CONTEXT);
            leftNorm = leftNorm.add(leftValue.multiply(leftValue, MATH_CONTEXT), MATH_CONTEXT);
        }
        for (BigDecimal value : right.values()) {
            BigDecimal safeValue = safe(value);
            rightNorm = rightNorm.add(safeValue.multiply(safeValue, MATH_CONTEXT), MATH_CONTEXT);
        }
        if (BigDecimal.ZERO.compareTo(leftNorm) == 0 || BigDecimal.ZERO.compareTo(rightNorm) == 0) {
            return BigDecimal.ZERO;
        }
        double denominator = Math.sqrt(leftNorm.doubleValue()) * Math.sqrt(rightNorm.doubleValue());
        if (denominator == 0D) {
            return BigDecimal.ZERO;
        }
        return dot.divide(BigDecimal.valueOf(denominator), MATH_CONTEXT);
    }

    private static BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
