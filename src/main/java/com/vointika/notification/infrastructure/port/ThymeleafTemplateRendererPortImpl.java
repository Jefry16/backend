package com.vointika.notification.infrastructure.port;

import com.vointika.notification.application.port.TemplateRendererPort;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.util.Map;

@Component
public class ThymeleafTemplateRendererPortImpl implements TemplateRendererPort {

    private final SpringTemplateEngine templateEngine;

    public ThymeleafTemplateRendererPortImpl() {
        this.templateEngine = new SpringTemplateEngine();
        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode(TemplateMode.HTML);
        this.templateEngine.setTemplateResolver(resolver);
    }

    @Override
    public String render(String templateContent, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);
        return templateEngine.process(templateContent, context);
    }
}
