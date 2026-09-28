package com.mockingbirdbank.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AnalyticsEventServiceTest {

    @Test
    void trackIncrementsACounterTaggedByEventAndSuccessOutcome() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AnalyticsEventService analytics = new AnalyticsEventService(registry);

        analytics.track("dashboard.viewed", Map.of("accountCount", "3"));

        assertThat(
                        registry.get("product.events")
                                .tag("event", "dashboard.viewed")
                                .tag("outcome", "success")
                                .counter()
                                .count())
                .isEqualTo(1.0);
    }

    @Test
    void trackErrorIncrementsACounterTaggedByReason() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AnalyticsEventService analytics = new AnalyticsEventService(registry);

        analytics.trackError("account.viewed", "not_found", Map.of("accountId", "999"));

        assertThat(
                        registry.get("product.events")
                                .tag("event", "account.viewed")
                                .tag("outcome", "error")
                                .tag("reason", "not_found")
                                .counter()
                                .count())
                .isEqualTo(1.0);
    }

    @Test
    void repeatedCallsAccumulateOnTheSameCounter() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AnalyticsEventService analytics = new AnalyticsEventService(registry);

        analytics.track("dashboard.viewed", Map.of());
        analytics.track("dashboard.viewed", Map.of());
        analytics.track("dashboard.viewed", Map.of());

        assertThat(
                        registry.get("product.events")
                                .tag("event", "dashboard.viewed")
                                .tag("outcome", "success")
                                .counter()
                                .count())
                .isEqualTo(3.0);
    }
}
