package com.mockingbirdbank.config;

import java.util.Map;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

/**
 * Exposes current feature-flag values, read-only, at {@code /actuator/featureflags} - so "what's
 * actually turned on in this environment right now" is one authenticated request away instead of
 * needing to compare deployed env vars against application.yml defaults by hand.
 */
@Component
@Endpoint(id = "featureflags")
public class FeatureFlagsEndpoint {

    private final FeatureFlagService featureFlagService;

    public FeatureFlagsEndpoint(FeatureFlagService featureFlagService) {
        this.featureFlagService = featureFlagService;
    }

    @ReadOperation
    public Map<String, Boolean> flags() {
        return featureFlagService.allFlags();
    }
}
