package com.mockingbirdbank.config.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.Test;

class ScrubbingMessageConverterTest {

    private final ScrubbingMessageConverter converter = new ScrubbingMessageConverter();

    private String convert(String formattedMessage) {
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getFormattedMessage()).thenReturn(formattedMessage);
        return converter.convert(event);
    }

    @Test
    void redactsPasswordKeyValuePair() {
        assertThat(convert("Login attempt with password=SuperSecret123"))
                .isEqualTo("Login attempt with password=***REDACTED***");
    }

    @Test
    void redactsTokenWithColonSeparator() {
        assertThat(convert("Calling upstream API, token: abc.def-GHI_123"))
                .isEqualTo("Calling upstream API, token=***REDACTED***");
    }

    @Test
    void redactsAuthorizationBearerHeader() {
        assertThat(convert("Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.payload.sig"))
                .isEqualTo("Authorization: Bearer ***REDACTED***");
    }

    @Test
    void redactsCardLikeNumberSequences() {
        assertThat(convert("Charged card 4111 1111 1111 1111 successfully"))
                .isEqualTo("Charged card ***REDACTED*** successfully");
    }

    @Test
    void leavesOrdinaryMessagesUnchanged() {
        String message = "Seeded DEMO login for local/dev use only - username: jordan.ellis";
        assertThat(convert(message)).isEqualTo(message);
    }

    @Test
    void handlesNullAndEmptyMessagesWithoutThrowing() {
        assertThat(convert(null)).isNull();
        assertThat(convert("")).isEmpty();
    }
}
