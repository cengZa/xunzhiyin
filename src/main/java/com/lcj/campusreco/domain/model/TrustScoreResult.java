package com.lcj.campusreco.domain.model;

import java.math.BigDecimal;
import java.util.List;

public record TrustScoreResult(BigDecimal score, List<String> reasons) {
}
