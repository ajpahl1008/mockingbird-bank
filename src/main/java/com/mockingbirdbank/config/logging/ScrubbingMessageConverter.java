package com.mockingbirdbank.config.logging;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import java.util.regex.Pattern;

/**
 * Redacts likely-sensitive values from every formatted log message, repo-wide, before Logback
 * writes it anywhere - see {@code logback-spring.xml}, which registers this in place of the
 * built-in {@code %msg}/{@code %m} conversion word.
 *
 * <p>This is a safety net, not a substitute for not logging secrets in the first place (see {@code
 * DataInitializer}, which used to log the seeded demo password directly): a future {@code
 * log.info("...", someObjectThatHappensToContainAToken)} shouldn't be able to leak a credential
 * just because nobody thought to scrub it at the call site.
 */
public class ScrubbingMessageConverter extends MessageConverter {

    // "Authorization: Bearer <token>" / "Authorization: Basic <credentials>" headers logged
    // verbatim (e.g. by an HTTP client's debug logging). Checked before KEY_VALUE_SECRET below
    // so "authorization" doesn't get treated as a plain key=value pair first, which would
    // redact only the word "Bearer"/"Basic" and leave the actual credential untouched.
    private static final Pattern AUTH_HEADER =
            Pattern.compile("(?i)(authorization\\s*:\\s*(?:bearer|basic)\\s+)(\\S+)");

    // key=value / key: value pairs for common secret-shaped keys. Matches up to the next
    // whitespace/comma/quote/bracket so it doesn't eat the rest of the log line.
    private static final Pattern KEY_VALUE_SECRET =
            Pattern.compile(
                    "(?i)\\b(password|passwd|pwd|secret|token|api[_-]?key|access[_-]?token"
                            + "|refresh[_-]?key|refresh[_-]?token)"
                            + "\\s*[=:]\\s*['\"]?([^\\s,'\"\\]}]+)");

    // 13-19 digits total, optionally separated by spaces/dashes between digits (never trailing,
    // so a following character - e.g. the rest of the sentence - isn't swallowed) - long enough
    // to be a card number, not just any numeric ID. Intentionally coarse; a false positive just
    // over-redacts.
    private static final Pattern CARD_NUMBER = Pattern.compile("\\b\\d(?:[ -]?\\d){12,18}\\b");

    private static final String REDACTED = "***REDACTED***";

    @Override
    public String convert(ILoggingEvent event) {
        String message = super.convert(event);
        if (message == null || message.isEmpty()) {
            return message;
        }
        message = AUTH_HEADER.matcher(message).replaceAll("$1" + REDACTED);
        message = KEY_VALUE_SECRET.matcher(message).replaceAll("$1=" + REDACTED);
        message = CARD_NUMBER.matcher(message).replaceAll(REDACTED);
        return message;
    }
}
