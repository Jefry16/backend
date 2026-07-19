package com.vointika.notification.application.port;

import java.util.Map;

public interface TemplateRendererPort {
    String render(String templateContent, Map<String, Object> variables);
}
