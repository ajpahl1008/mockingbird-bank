package com.mockingbirdbank.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;

class AuthenticationEventListenerTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final AnalyticsEventService analytics = new AnalyticsEventService(registry);
    private final AuthenticationEventListener listener = new AuthenticationEventListener(analytics);

    @Test
    void successfulLoginIsTrackedAsASuccessOutcome() {
        Authentication authentication = new TestingAuthenticationToken("jordan.ellis", "n/a");

        listener.onSuccess(new AuthenticationSuccessEvent(authentication));

        assertThat(
                        registry.get("product.events")
                                .tag("event", "user.login")
                                .tag("outcome", "success")
                                .counter()
                                .count())
                .isEqualTo(1.0);
    }

    @Test
    void failedLoginIsTrackedWithTheExceptionTypeAsTheReason() {
        Authentication authentication = new TestingAuthenticationToken("jordan.ellis", "wrong");

        listener.onFailure(
                new AuthenticationFailureBadCredentialsEvent(
                        authentication, new BadCredentialsException("bad credentials")));

        assertThat(
                        registry.get("product.events")
                                .tag("event", "user.login")
                                .tag("outcome", "error")
                                .tag("reason", "BadCredentialsException")
                                .counter()
                                .count())
                .isEqualTo(1.0);
    }
}
