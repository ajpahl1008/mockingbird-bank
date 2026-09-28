package com.mockingbirdbank.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.ErrorHandler;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.SessionInitEvent;
import com.vaadin.flow.server.SessionInitListener;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GlobalErrorHandlerTest {

    @Test
    void wiresASessionErrorHandlerThatTracksThenDelegates() throws Exception {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AnalyticsEventService analytics = new AnalyticsEventService(registry);
        GlobalErrorHandler handler = new GlobalErrorHandler(analytics);

        VaadinService service = mock(VaadinService.class);
        ServiceInitEvent serviceInitEvent = mock(ServiceInitEvent.class);
        when(serviceInitEvent.getSource()).thenReturn(service);

        handler.serviceInit(serviceInitEvent);

        ArgumentCaptor<SessionInitListener> listenerCaptor =
                ArgumentCaptor.forClass(SessionInitListener.class);
        verify(service).addSessionInitListener(listenerCaptor.capture());

        VaadinSession session = mock(VaadinSession.class);
        SessionInitEvent sessionInitEvent = mock(SessionInitEvent.class);
        when(sessionInitEvent.getSession()).thenReturn(session);

        listenerCaptor.getValue().sessionInit(sessionInitEvent);

        ArgumentCaptor<ErrorHandler> errorHandlerCaptor =
                ArgumentCaptor.forClass(ErrorHandler.class);
        verify(session).setErrorHandler(errorHandlerCaptor.capture());

        errorHandlerCaptor.getValue().error(new ErrorEvent(new IllegalStateException("boom")));

        assertThat(
                        registry.get("product.events")
                                .tag("event", "ui.unhandled_exception")
                                .tag("outcome", "error")
                                .tag("reason", "IllegalStateException")
                                .counter()
                                .count())
                .isEqualTo(1.0);
    }
}
