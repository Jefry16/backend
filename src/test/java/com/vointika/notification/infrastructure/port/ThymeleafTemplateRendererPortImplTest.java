package com.vointika.notification.infrastructure.port;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ThymeleafTemplateRendererPortImplTest {

    private ThymeleafTemplateRendererPortImpl renderer;

    @BeforeEach
    void setUp() {
        renderer = new ThymeleafTemplateRendererPortImpl();
    }

    @Test
    void shouldRenderVariables() {
        String template = "Hello <span th:text=\"${name}\">placeholder</span>";
        String result = renderer.render(template, Map.of("name", "John"));
        assertTrue(result.contains("John"));
        assertFalse(result.contains("placeholder"));
    }

    @Test
    void shouldRenderLink() {
        String template = "<a th:href=\"${link}\">Click here</a>";
        String result = renderer.render(template, Map.of("link", "http://example.com/verify?token=abc"));
        assertTrue(result.contains("http://example.com/verify?token=abc"));
    }

    @Test
    void shouldHandleConditional() {
        String template = "<p th:if=\"${showExpiry}\">Expires in 24 hours</p>";

        String withExpiry = renderer.render(template, Map.of("showExpiry", true));
        assertTrue(withExpiry.contains("Expires in 24 hours"));

        String withoutExpiry = renderer.render(template, Map.of("showExpiry", false));
        assertFalse(withoutExpiry.contains("Expires in 24 hours"));
    }
}
