package com.vointika.storefront.presentation.controller;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The dump is off unless switched on, and <b>that is the half worth testing</b>:
 * enabled it is a development convenience, disabled it is the difference between
 * a public page and a public dump of everything the envelope carries whether the
 * page renders it or not.
 */
class ThemeContextDumpTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public record Shop(String name, String timezone) {}

    @Test
    void disabledTheParameterIsNotAParameter() {
        assertThat(new ThemeContextDump(MAPPER, false).requestedIn(withFormat("json"))).isFalse();
    }

    @Test
    void enabledItAnswersOnlyForTheJsonFormat() {
        ThemeContextDump dump = new ThemeContextDump(MAPPER, true);

        assertThat(dump.requestedIn(withFormat("json"))).isTrue();
        assertThat(dump.requestedIn(withFormat("xml"))).isFalse();
        assertThat(dump.requestedIn(withFormat(null))).isFalse();
    }

    /**
     * It serializes the view object itself, so what a theme sees and what this
     * prints cannot drift — a field added to the contract appears here with no
     * change to this class.
     */
    @Test
    void itSerializesTheViewTheTemplateWouldHaveRendered() {
        assertThat(new ThemeContextDump(MAPPER, true).of(new Shop("Acme Tours", "Europe/Madrid")))
                .contains("\"name\" : \"Acme Tours\"")
                .contains("\"timezone\" : \"Europe/Madrid\"");
    }

    private static MockHttpServletRequest withFormat(String format) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (format != null) {
            request.setParameter("format", format);
        }
        return request;
    }
}
