package com.vointika.notification.infrastructure.port;

import com.vointika.notification.application.port.EmailSenderPort;
import com.vointika.notification.infrastructure.config.SesProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;

@Component
public class SesEmailSenderPortImpl implements EmailSenderPort {

    private static final Logger log = LoggerFactory.getLogger(SesEmailSenderPortImpl.class);

    /**
     * Bounded retry for transient send failures. The AWS SDK retries within a
     * single call, but a short DNS/connectivity blackout exhausts those and
     * surfaces as an {@link SdkClientException} — re-sending a few seconds later
     * usually succeeds. Only client-side (network/DNS/timeout) failures are
     * retried; service rejections (e.g. an unverified recipient) are permanent
     * and propagate immediately. Every caller is an {@code @Async} notification
     * listener, so the backoff never blocks a request thread.
     */
    private static final int MAX_ATTEMPTS = 3;
    private static final long BASE_BACKOFF_MS = 500;

    private final SesV2Client sesClient;
    private final String fromAddress;

    public SesEmailSenderPortImpl(SesProperties sesProperties) {
        this.sesClient = SesV2Client.builder()
                .region(Region.of(sesProperties.region()))
                .build();
        this.fromAddress = sesProperties.fromAddress();
    }

    @Override
    public void send(String to, String subject, String htmlBody,
                     String senderDisplayName, String replyTo) {
        SendEmailRequest.Builder builder = SendEmailRequest.builder()
                .destination(d -> d.toAddresses(to))
                .fromEmailAddress(buildFromHeader(senderDisplayName, fromAddress))
                .content(c -> c.simple(s -> s
                        .subject(sub -> sub.data(subject).charset("UTF-8"))
                        .body(b -> b.html(h -> h.data(htmlBody).charset("UTF-8")))));
        if (replyTo != null && !replyTo.isBlank()) {
            builder.replyToAddresses(replyTo);
        }
        SendEmailRequest request = builder.build();

        SdkClientException last = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                sesClient.sendEmail(request);
                return;
            } catch (SdkClientException e) {
                // Transient (connection/DNS/timeout). Service rejections are
                // SesV2Exception/SdkServiceException — not caught here, so they
                // propagate without a pointless retry.
                last = e;
                if (attempt == MAX_ATTEMPTS) {
                    break;
                }
                log.warn("SES send to {} failed (attempt {}/{}), retrying: {}",
                        to, attempt, MAX_ATTEMPTS, e.getMessage());
                try {
                    Thread.sleep(BASE_BACKOFF_MS * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        throw last;
    }

    /**
     * {@code "Display Name" <from>} — or the bare from-address when no (or an
     * empty-after-sanitizing) display name is given, which keeps the
     * platform-branded path byte-identical. The header-injection guard: CR/LF
     * and {@code <>} are STRIPPED (never encoded — a folded header is an
     * injection vector), quotes/backslashes are removed rather than escaped
     * (display names, not code), and the result is capped defensively. The
     * quoted-string form keeps commas/periods in operator names RFC-5322-safe;
     * SES handles the RFC-2047 encoding of non-ASCII itself.
     */
    static String buildFromHeader(String senderDisplayName, String fromAddress) {
        if (senderDisplayName == null) {
            return fromAddress;
        }
        String cleaned = senderDisplayName
                .replaceAll("[\\r\\n<>\"\\\\]", "")
                .trim();
        if (cleaned.isEmpty()) {
            return fromAddress;
        }
        if (cleaned.length() > 80) {
            cleaned = cleaned.substring(0, 80).trim();
        }
        return "\"" + cleaned + "\" <" + fromAddress + ">";
    }
}
