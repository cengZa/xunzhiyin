package com.lcj.campusreco.config;

import com.lcj.campusreco.common.constant.RecommendationScenarioMode;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RecommendationTuningContext {

    private final int defaultProfileTopTagLimit;
    private final BigDecimal defaultRerankWeightScale;
    private final String defaultScenarioMode;
    private final boolean defaultTrustEnabled;
    private final ThreadLocal<OverrideValues> overrides = new ThreadLocal<>();

    @Autowired
    public RecommendationTuningContext(
            @Value("${app.recommendation.profile-top-tag-limit:5}") int defaultProfileTopTagLimit,
            @Value("${app.recommendation.rerank-weight-scale:1.0}") BigDecimal defaultRerankWeightScale,
            @Value("${app.recommendation.default-scenario-mode:interest_partner}") String defaultScenarioMode,
            @Value("${app.recommendation.trust-enabled:true}") boolean defaultTrustEnabled) {
        this.defaultProfileTopTagLimit = defaultProfileTopTagLimit;
        this.defaultRerankWeightScale = defaultRerankWeightScale;
        this.defaultScenarioMode = RecommendationScenarioMode.normalize(defaultScenarioMode);
        this.defaultTrustEnabled = defaultTrustEnabled;
    }

    public RecommendationTuningContext(int defaultProfileTopTagLimit, BigDecimal defaultRerankWeightScale) {
        this(defaultProfileTopTagLimit, defaultRerankWeightScale, RecommendationScenarioMode.INTEREST_PARTNER, true);
    }

    public int getProfileTopTagLimit() {
        OverrideValues values = overrides.get();
        if (values != null && values.profileTopTagLimit != null && values.profileTopTagLimit > 0) {
            return values.profileTopTagLimit;
        }
        return defaultProfileTopTagLimit;
    }

    public BigDecimal getRerankWeightScale() {
        OverrideValues values = overrides.get();
        if (values != null && values.rerankWeightScale != null && values.rerankWeightScale.signum() > 0) {
            return values.rerankWeightScale;
        }
        return defaultRerankWeightScale;
    }

    public String getScenarioMode() {
        OverrideValues values = overrides.get();
        if (values != null && values.scenarioMode != null && !values.scenarioMode.isBlank()) {
            return RecommendationScenarioMode.normalize(values.scenarioMode);
        }
        return defaultScenarioMode;
    }

    public boolean isTrustEnabled() {
        OverrideValues values = overrides.get();
        if (values != null && values.trustEnabled != null) {
            return values.trustEnabled;
        }
        return defaultTrustEnabled;
    }

    public Scope withOverrides(Integer profileTopTagLimit, BigDecimal rerankWeightScale) {
        return withOverrides(profileTopTagLimit, rerankWeightScale, null, null);
    }

    public Scope withOverrides(Integer profileTopTagLimit, BigDecimal rerankWeightScale, String scenarioMode) {
        return withOverrides(profileTopTagLimit, rerankWeightScale, scenarioMode, null);
    }

    public Scope withOverrides(Integer profileTopTagLimit,
                               BigDecimal rerankWeightScale,
                               String scenarioMode,
                               Boolean trustEnabled) {
        OverrideValues previous = overrides.get();
        overrides.set(new OverrideValues(profileTopTagLimit, rerankWeightScale, scenarioMode, trustEnabled));
        return new Scope(previous);
    }

    public final class Scope implements AutoCloseable {
        private final OverrideValues previous;

        private Scope(OverrideValues previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            if (previous == null) {
                overrides.remove();
            } else {
                overrides.set(previous);
            }
        }
    }

    private record OverrideValues(Integer profileTopTagLimit,
                                  BigDecimal rerankWeightScale,
                                  String scenarioMode,
                                  Boolean trustEnabled) {
    }
}
