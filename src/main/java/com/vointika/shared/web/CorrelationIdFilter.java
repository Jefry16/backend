package com.vointika.shared.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Assigns each request a correlation id, exposes it through:
 * <ul>
 *   <li>the {@code requestId} key in SLF4J MDC (visible in every log line via
 *       the {@code %X{requestId}} pattern token)</li>
 *   <li>the {@code X-Request-Id} response header so clients can link their logs
 *       to server logs</li>
 * </ul>
 *
 * <p>If the incoming request already carries an {@code X-Request-Id} header
 * (e.g. from a gateway or upstream service) it is adopted as-is; otherwise a
 * fresh UUID is generated.
 *
 * <p>Runs with {@link Ordered#HIGHEST_PRECEDENCE} so authentication failures,
 * request-size rejections and framework exceptions all inherit the id.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String REQUEST_ID_MDC_KEY = "requestId";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String incoming = request.getHeader(REQUEST_ID_HEADER);
        String requestId = (incoming != null && !incoming.isBlank())
                ? incoming
                : UUID.randomUUID().toString();

        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(REQUEST_ID_MDC_KEY);
        }
    }
}
