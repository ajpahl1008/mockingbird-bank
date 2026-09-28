package com.mockingbirdbank.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.mockingbirdbank.observability.CorrelationIdFilter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

class AnalyticsEventServiceTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

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

    @Test
    void eventLogLineIncludesTheCurrentCorrelationIdWhenOneIsSet() {
        Logger eventLogger =
                (Logger) LoggerFactory.getLogger("com.mockingbirdbank.analytics.events");
        ListAppender<ILoggingEvent> capturedLogs = new ListAppender<>();
        capturedLogs.start();
        eventLogger.addAppender(capturedLogs);
        try {
            // Deliberately not UUID/token-shaped - a longer, key-shaped literal here trips
            // gitleaks' generic-api-key heuristic as a false positive (any value works for this
            // assertion; only its presence in the log line matters).
            MDC.put(CorrelationIdFilter.MDC_KEY, "test-id");
            AnalyticsEventService analytics = new AnalyticsEventService(new SimpleMeterRegistry());

            analytics.track("dashboard.viewed", Map.of());

            assertThat(capturedLogs.list)
                    .extracting(ILoggingEvent::getFormattedMessage)
                    .anySatisfy(message -> assertThat(message).contains("correlationId=test-id"));
        } finally {
            eventLogger.detachAppender(capturedLogs);
        }
    }

    @Test
    void eventLogLineOmitsCorrelationIdWhenNoneIsSet() {
        Logger eventLogger =
                (Logger) LoggerFactory.getLogger("com.mockingbirdbank.analytics.events");
        ListAppender<ILoggingEvent> capturedLogs = new ListAppender<>();
        capturedLogs.start();
        eventLogger.addAppender(capturedLogs);
        try {
            AnalyticsEventService analytics = new AnalyticsEventService(new SimpleMeterRegistry());

            analytics.track("dashboard.viewed", Map.of());

            assertThat(capturedLogs.list)
                    .extracting(ILoggingEvent::getFormattedMessage)
                    .noneSatisfy(message -> assertThat(message).contains("correlationId="));
        } finally {
            eventLogger.detachAppender(capturedLogs);
        }
    }
}
