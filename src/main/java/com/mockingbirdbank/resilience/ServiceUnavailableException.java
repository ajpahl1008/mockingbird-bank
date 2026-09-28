package com.mockingbirdbank.resilience;

/**
 * Thrown when a service's circuit breaker is open, i.e. its underlying repository has already been
 * failing enough that we've stopped even trying the call and are failing fast instead. Distinct
 * from a plain repository exception (which still gets one real attempt) so a UI, or a log/metrics
 * consumer, can tell "the database itself just threw" apart from "we're deliberately not hitting
 * the database right now."
 */
public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
