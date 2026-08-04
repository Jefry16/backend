package com.vointika.storefront.presentation.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Renders the object a template receives as JSON instead of HTML, when
 * {@code ?format=json} is asked for and the endpoint is switched on.
 *
 * <p><b>Why it exists.</b> The {@code shop} contract is filled in data-first —
 * a field is admitted because a column backs it, not because something renders
 * it yet — so most of the envelope is invisible in the page. Without this there
 * is no way to see what a theme actually receives short of adding a template
 * line per field, and no way to check the contract against a running system at
 * all. It is the diagnostic that makes data-first verifiable.
 *
 * <p><b>Off unless enabled</b> ({@code app.storefront.context-endpoint}, default
 * {@code false}; the dev compose file turns it on). It is a development tool, not
 * a public API: serving it always would make every field in the envelope
 * permanently public whether a page renders it or not, which is a commitment the
 * contract should not make by accident. A theme author's version of this belongs
 * in generated documentation, and is a separate decision.
 *
 * <p>It is a query parameter on the page routes rather than a route of its own,
 * and that is load-bearing twice: a storefront route has to be registered in
 * <b>four</b> places to work, and — more importantly — the password gate is an
 * interceptor on those routes, so the dump is behind the gate for free. A locked
 * store answers {@code ?format=json} with the same {@code 302} it answers
 * everything else.
 */
@Component
public class ThemeContextDump {

    private final ObjectMapper objectMapper;
    private final boolean enabled;

    public ThemeContextDump(ObjectMapper objectMapper,
                            @Value("${app.storefront.context-endpoint:false}") boolean enabled) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
    }

    /** Disabled, the parameter is simply not a parameter and the page renders. */
    public boolean requestedIn(HttpServletRequest request) {
        return enabled && "json".equals(request.getParameter("format"));
    }

    public String of(Object view) {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(view);
    }
}
