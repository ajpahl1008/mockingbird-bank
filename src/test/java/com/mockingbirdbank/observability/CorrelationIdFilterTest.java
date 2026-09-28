package com.mockingbirdbank.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void generatesAFreshCorrelationIdWhenTheCallerDidntSendOne() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> seenDuringRequest = new AtomicReference<>();
        MockFilterChain chain =
                new MockFilterChain() {
                    @Override
                    public void doFilter(
                            jakarta.servlet.ServletRequest req,
                            jakarta.servlet.ServletResponse res) {
                        seenDuringRequest.set(MDC.get(CorrelationIdFilter.MDC_KEY));
                    }
                };

        filter.doFilter(request, response, chain);

        assertThat(seenDuringRequest.get()).isNotBlank();
        assertThat(response.getHeader(CorrelationIdFilter.HEADER))
                .isEqualTo(seenDuringRequest.get());
        // MDC is thread-local state that must not leak into whatever request/test runs next.
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void reusesAnIncomingCorrelationIdInsteadOfGeneratingANewOne() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, "from-upstream-service-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(CorrelationIdFilter.HEADER))
                .isEqualTo("from-upstream-service-123");
    }

    @Test
    void clearsMdcEvenWhenTheRestOfTheChainThrows() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain =
                new MockFilterChain() {
                    @Override
                    public void doFilter(
                            jakarta.servlet.ServletRequest req,
                            jakarta.servlet.ServletResponse res) {
                        throw new RuntimeException("downstream failure");
                    }
                };

        org.junit.jupiter.api.Assertions.assertThrows(
                RuntimeException.class, () -> filter.doFilter(request, response, chain));

        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }
}
