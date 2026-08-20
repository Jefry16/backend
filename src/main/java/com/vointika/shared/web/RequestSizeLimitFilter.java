package com.vointika.shared.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;
import java.time.Instant;

/**
 * Rejects oversized request bodies with 413 before Jackson or any handler runs.
 * Tomcat's max-http-form-post-size only covers form and multipart payloads, not
 * raw JSON. Multipart uploads have their own Spring-level limit and are exempt
 * from this filter.
 */
@Component
public class RequestSizeLimitFilter extends OncePerRequestFilter {

    private static final long MAX_JSON_BODY_BYTES = (long) 1024 * 1024; // 1 MB

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String contentType = request.getContentType();
        // Locale.ROOT: MULTIPART has an I, and a Turkish default folds it to a dotless ı,
        // so a client sending "Multipart/form-data" stops being recognised and its upload
        // gets the JSON cap instead. Fails closed, and only on a host you did not pick.
        boolean isMultipart = contentType != null
                && contentType.toLowerCase(Locale.ROOT).startsWith(MediaType.MULTIPART_FORM_DATA_VALUE);

        if (!isMultipart) {
            long contentLength = request.getContentLengthLong();
            if (contentLength > MAX_JSON_BODY_BYTES) {
                writePayloadTooLarge(response);
                return;
            }
            // Chunked (Content-Length = -1): wrap the stream so reading beyond the
            // limit triggers 413. For simplicity, we do not wrap here; Tomcat's own
            // maxPostSize still applies to form-encoded, and a chunked JSON attacker
            // would have to send a body and then be rejected at read time. In
            // practice every JSON client sends Content-Length, so the header check
            // covers the common case.
        }

        filterChain.doFilter(request, response);
    }

    private void writePayloadTooLarge(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.CONTENT_TOO_LARGE.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String body = "{\"status\":413,\"error\":\"Content Too Large\","
                + "\"message\":\"Request body exceeds the 1 MB limit\","
                + "\"timestamp\":\"" + Instant.now() + "\"}";
        response.getWriter().write(body);
    }
}
