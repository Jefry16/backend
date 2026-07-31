package com.vointika.notification.infrastructure.config;

import com.vointika.shared.port.DiagnosticLogPort;
import com.vointika.notification.application.port.EmailSenderPort;
import com.vointika.notification.application.port.TemplateCatalog;
import com.vointika.notification.application.port.TemplateRendererPort;
import com.vointika.notification.application.usecase.SendNotificationUseCase;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration("notificationUseCaseConfig")
@EnableConfigurationProperties({SesProperties.class, NotificationProperties.class})
public class UseCaseConfig {

    @Bean
    public SendNotificationUseCase sendNotificationUseCase(
            TemplateCatalog templateCatalog,
            TemplateRendererPort templateRenderer,
            EmailSenderPort emailSender, DiagnosticLogPort diagnosticLog) {
        return new SendNotificationUseCase(templateCatalog, templateRenderer, emailSender, diagnosticLog);
    }
}
