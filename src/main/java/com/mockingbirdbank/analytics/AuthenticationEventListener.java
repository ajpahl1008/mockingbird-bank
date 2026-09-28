package com.mockingbirdbank.analytics;

import java.util.Map;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * Spring Security publishes an {@link AuthenticationSuccessEvent} or one of the {@link
 * AbstractAuthenticationFailureEvent} subclasses on every login attempt regardless of which {@code
 * AuthenticationProvider} handled it - this just listens for both and forwards them to {@link
 * AnalyticsEventService}, so "how many people are signing in" and "how often are people failing to"
 * are both real, queryable numbers instead of something only visible by tailing logs by hand.
 */
@Component
public class AuthenticationEventListener {

    private final AnalyticsEventService analytics;

    public AuthenticationEventListener(AnalyticsEventService analytics) {
        this.analytics = analytics;
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        analytics.track("user.login", Map.of("username", event.getAuthentication().getName()));
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        String reason = event.getException().getClass().getSimpleName();
        analytics.trackError(
                "user.login",
                reason,
                Map.of("username", String.valueOf(event.getAuthentication().getName())));
    }
}
