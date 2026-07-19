package com.vointika.shared.web;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void generatesUuidWhenNoIncomingHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> mdcDuringChain = new AtomicReference<>();
        FilterChain chain = (req, res) -> mdcDuringChain.set(MDC.get(CorrelationIdFilter.REQUEST_ID_MDC_KEY));

        filter.doFilter(request, response, chain);

        String echoed = response.getHeader(CorrelationIdFilter.REQUEST_ID_HEADER);
        assertNotNull(echoed);
        assertDoesNotThrow(() -> UUID.fromString(echoed));
        assertEquals(echoed, mdcDuringChain.get(), "MDC during chain must match echoed header");
    }

    @Test
    void adoptsIncomingHeaderVerbatim() throws Exception {
        String upstream = "upstream-trace-123";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.REQUEST_ID_HEADER, upstream);
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> mdcDuringChain = new AtomicReference<>();
        FilterChain chain = (req, res) -> mdcDuringChain.set(MDC.get(CorrelationIdFilter.REQUEST_ID_MDC_KEY));

        filter.doFilter(request, response, chain);

        assertEquals(upstream, response.getHeader(CorrelationIdFilter.REQUEST_ID_HEADER));
        assertEquals(upstream, mdcDuringChain.get());
    }

    @Test
    void generatesNewIdWhenIncomingHeaderIsBlank() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.REQUEST_ID_HEADER, "   ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {});

        String echoed = response.getHeader(CorrelationIdFilter.REQUEST_ID_HEADER);
        assertNotNull(echoed);
        assertNotEquals("   ", echoed);
        assertDoesNotThrow(() -> UUID.fromString(echoed));
    }

    @Test
    void cleansMdcAfterChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {});

        assertNull(MDC.get(CorrelationIdFilter.REQUEST_ID_MDC_KEY));
    }

    @Test
    void cleansMdcEvenWhenChainThrows() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThrows(RuntimeException.class, () ->
                filter.doFilter(request, response, (req, res) -> {
                    throw new RuntimeException("boom");
                }));

        assertNull(MDC.get(CorrelationIdFilter.REQUEST_ID_MDC_KEY));
    }
}
