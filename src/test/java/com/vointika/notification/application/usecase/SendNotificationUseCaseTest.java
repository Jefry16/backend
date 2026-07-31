package com.vointika.notification.application.usecase;

import com.vointika.shared.port.DiagnosticLogPort;
import com.vointika.notification.application.port.EmailSenderPort;
import com.vointika.notification.application.port.TemplateCatalog;
import com.vointika.notification.application.port.TemplateCatalog.EmailTemplate;
import com.vointika.notification.application.port.TemplateRendererPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SendNotificationUseCaseTest {

    @Mock private TemplateCatalog templateCatalog;
    @Mock private TemplateRendererPort templateRenderer;
    @Mock private EmailSenderPort emailSender;
    @Mock private DiagnosticLogPort diagnosticLog;

    private SendNotificationUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new SendNotificationUseCase(templateCatalog, templateRenderer, emailSender, diagnosticLog);
    }

    private static EmailTemplate template(String locale, String subject) {
        return new EmailTemplate(locale, subject, "<p>body</p>");
    }

    @Test
    void threeArgOverloadUsesTheEnglishTemplate() {
        when(templateCatalog.find("ORDER_CONFIRMATION_EMAIL", "en"))
                .thenReturn(Optional.of(template("en", "Hello ${name}")));
        when(templateRenderer.render("Hello ${name}", Map.of("name", "John")))
                .thenReturn("Hello John");
        when(templateRenderer.render("<p>body</p>", Map.of("name", "John")))
                .thenReturn("<p>rendered</p>");

        useCase.execute("ORDER_CONFIRMATION_EMAIL", "test@example.com", Map.of("name", "John"));

        verify(emailSender).send("test@example.com", "Hello John", "<p>rendered</p>", null, null);
    }

    @Test
    void exactLocaleWins() {
        when(templateCatalog.find("ORDER_CONFIRMATION_EMAIL", "es"))
                .thenReturn(Optional.of(template("es", "Hola")));
        when(templateRenderer.render(any(), any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute("ORDER_CONFIRMATION_EMAIL", "t@example.com", Map.of(), "es");

        verify(emailSender).send("t@example.com", "Hola", "<p>body</p>", null, null);
        // The English template is never consulted when the exact locale hits.
        verify(templateCatalog, never()).find("ORDER_CONFIRMATION_EMAIL", "en");
    }

    @Test
    void regionalTagFallsBackToItsPrimarySubtag() {
        when(templateCatalog.find("ORDER_CONFIRMATION_EMAIL", "pt-br"))
                .thenReturn(Optional.empty());
        when(templateCatalog.find("ORDER_CONFIRMATION_EMAIL", "pt"))
                .thenReturn(Optional.of(template("pt", "Olá")));
        when(templateRenderer.render(any(), any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute("ORDER_CONFIRMATION_EMAIL", "t@example.com", Map.of(), "pt-br");

        verify(emailSender).send("t@example.com", "Olá", "<p>body</p>", null, null);
    }

    @Test
    void unknownLocaleFallsBackToEnglish() {
        when(templateCatalog.find("ORDER_CONFIRMATION_EMAIL", "xx"))
                .thenReturn(Optional.empty());
        when(templateCatalog.find("ORDER_CONFIRMATION_EMAIL", "en"))
                .thenReturn(Optional.of(template("en", "Hello")));
        when(templateRenderer.render(any(), any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute("ORDER_CONFIRMATION_EMAIL", "t@example.com", Map.of(), "xx");

        verify(emailSender).send("t@example.com", "Hello", "<p>body</p>", null, null);
    }

    @Test
    void senderDisplayNameAndReplyToPassThroughToTheEmailSender() {
        when(templateCatalog.find("ORDER_CONFIRMATION_EMAIL", "en"))
                .thenReturn(Optional.of(template("en", "Hello")));
        when(templateRenderer.render(any(), any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute("ORDER_CONFIRMATION_EMAIL", "t@example.com", Map.of(), null,
                "Acme Tours via Vointika", "hello@acme.example");

        verify(emailSender).send("t@example.com", "Hello", "<p>body</p>",
                "Acme Tours via Vointika", "hello@acme.example");
    }

    @Test
    void shouldNotSendWhenNoTemplateFound() {
        when(templateCatalog.find("UNKNOWN", "en")).thenReturn(Optional.empty());

        useCase.execute("UNKNOWN", "test@example.com", Map.of());

        verify(templateRenderer, never()).render(any(), any());
        verify(emailSender, never()).send(any(), any(), any(), any(), any());
    }
}
