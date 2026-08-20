package com.vointika.shared.web.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.time.Instant;

/**
 * The 429 both rate-limit filters answer with.
 *
 * <p><b>Written as a string rather than serialized, because a filter runs before
 * any {@code @ExceptionHandler}</b> — there is no handler to map an exception to
 * {@code ApiErrorResponse} at this point in the chain, so the §6.1 error shape is
 * reproduced here by hand. That is the one real cost: this JSON and
 * {@code ApiErrorResponse} are two spellings of one shape, and nothing checks they
 * agree. Keeping the copy in <em>one</em> place at least means a drift is one edit
 * away from being fixed rather than two.
 *
 * <p>It used to be two copies — {@link EndpointRateLimitFilter} and
 * {@link ApiRateLimitFilter} each carried the same nine lines, and the second even
 * carried a comment pointing at the first for the reason. Two filters answering the
 * same condition differently is the failure this prevents.
 *
 * <p><b>The message is ASCII-only on purpose.</b> Bypassing Jackson means the bytes
 * depend on the writer's charset, so the body must not assume the container's
 * default is UTF-8 — the encoding is set explicitly here and the text stays inside
 * ASCII regardless.
 */
final class RateLimitRefusal {

    private RateLimitRefusal() {
    }

    static void write(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"status\":429,\"error\":\"Too Many Requests\","
                + "\"message\":\"Too many requests, try again later\","
                + "\"timestamp\":\"" + Instant.now() + "\"}");
    }
}
