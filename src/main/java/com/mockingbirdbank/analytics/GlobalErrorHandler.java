package com.mockingbirdbank.analytics;

import com.vaadin.flow.server.DefaultErrorHandler;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Registers a session-wide Vaadin {@code ErrorHandler} so an otherwise-unhandled exception in any
 * view - not just the ones with their own try/catch, like {@code AccountDetailView}'s 404 case -
 * turns into a real, aggregable signal instead of only being visible by tailing the application log
 * after a user reports "something broke": {@link AnalyticsEventService} increments the same {@code
 * product.events} counter (tagged {@code event=ui.unhandled_exception, reason=<ExceptionType>})
 * used for every other tracked event, so a spike in one exception type shows up the same way a
 * spike in "account view failures" would. Delegates the actual user-facing behavior - logging the
 * stack trace, showing the "an internal error has occurred" notification - to Vaadin's own {@link
 * DefaultErrorHandler}; this only adds the insight step on top of it.
 */
@Component
public class GlobalErrorHandler implements VaadinServiceInitListener {

    private final AnalyticsEventService analytics;
    private final DefaultErrorHandler delegate = new DefaultErrorHandler();

    public GlobalErrorHandler(AnalyticsEventService analytics) {
        this.analytics = analytics;
    }

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource()
                .addSessionInitListener(
                        sessionInitEvent ->
                                sessionInitEvent.getSession().setErrorHandler(this::handle));
    }

    private void handle(ErrorEvent errorEvent) {
        analytics.trackError(
                "ui.unhandled_exception",
                errorEvent.getThrowable().getClass().getSimpleName(),
                Map.of());
        delegate.error(errorEvent);
    }
}
