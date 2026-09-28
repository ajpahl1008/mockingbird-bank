package com.mockingbirdbank.analytics;

import com.mockingbirdbank.observability.CorrelationIdFilter;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

/**
 * Single call-site-facing API for recording two kinds of "what actually happened" signal that
 * neither the test suite nor the app-error logs capture on their own:
 *
 * <ul>
 *   <li><b>Product events</b> ({@link #track}) - which screens/features people actually use
 *       (dashboard viewed, an account opened), the input product decisions (a new feature flag, a
 *       redesign) need.
 *   <li><b>User-facing errors</b> ({@link #trackError}) - failures a real user hit (bad login, a
 *       404'd account), tagged with a reason so they're aggregable, not just one-off log lines.
 * </ul>
 *
 * Every call does two things, both real and queryable without any external SaaS:
 *
 * <ol>
 *   <li>Increments a Micrometer counter ({@code product.events}), tagged by event name and outcome
 *       - visible at {@code /actuator/metrics/product.events} today, and exportable to
 *       Prometheus/Datadog/etc. later by adding the matching Micrometer registry dependency, with
 *       zero call-site changes.
 *   <li>Writes one structured line to the {@code com.mockingbirdbank.analytics.events} logger,
 *       which {@code logback-spring.xml} routes to its own rolling file ({@code
 *       build/logs/analytics-events.log}) independent of the application's regular logs - a
 *       durable, greppable/shippable event stream a team without a dedicated analytics platform yet
 *       can point a log shipper at.
 * </ol>
 */
@Service
public class AnalyticsEventService {

    private static final Logger EVENT_LOG =
            LoggerFactory.getLogger("com.mockingbirdbank.analytics.events");

    private final MeterRegistry meterRegistry;

    public AnalyticsEventService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /** Records a successful product interaction, e.g. {@code "dashboard.viewed"}. */
    public void track(String eventName, Map<String, String> attributes) {
        record(eventName, "success", null, attributes);
    }

    /**
     * Records a user-facing failure, e.g. {@code trackError("account.view_failed", "not_found",
     * Map.of("accountId", id))}. {@code reason} is a short, low-cardinality label (not the raw
     * exception message) so the resulting counter stays a meaningful breakdown rather than one
     * series per occurrence.
     */
    public void trackError(String eventName, String reason, Map<String, String> attributes) {
        record(eventName, "error", reason, attributes);
    }

    private void record(
            String eventName, String outcome, String reason, Map<String, String> attributes) {
        Counter.Builder counter =
                Counter.builder("product.events")
                        .tag("event", eventName)
                        .tag("outcome", outcome)
                        .tag("reason", reason == null ? "n/a" : reason);
        counter.register(meterRegistry).increment();

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("event", eventName);
        fields.put("outcome", outcome);
        if (reason != null) {
            fields.put("reason", reason);
        }
        // Same ID CorrelationIdFilter put in MDC for this request/response - so this line is
        // traceable back to the exact request that produced it without relying on nearby
        // timestamps in a different log stream.
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        if (correlationId != null) {
            fields.put("correlationId", correlationId);
        }
        if (attributes != null) {
            fields.putAll(attributes);
        }
        EVENT_LOG.info(toLogLine(fields));
    }

    private static String toLogLine(Map<String, String> fields) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return sb.toString();
    }
}
