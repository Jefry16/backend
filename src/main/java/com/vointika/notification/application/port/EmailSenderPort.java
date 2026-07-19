package com.vointika.notification.application.port;

public interface EmailSenderPort {

    /**
     * Platform-branded send — the identity/team/operator emails. Delegates to
     * the extended form with no display name and no Reply-To, so its behavior
     * is byte-identical to before the extended form existed.
     */
    default void send(String to, String subject, String htmlBody) {
        send(to, subject, htmlBody, null, null);
    }

    /**
     * Operator-flavored send — the shopper emails. {@code senderDisplayName}
     * (nullable) becomes the From header's display name over the platform
     * from-address ({@code "Name <from>"}); the adapter must sanitize it
     * against header injection (strip CR/LF and {@code <>}). {@code replyTo}
     * (nullable) sets the Reply-To header; {@code null} omits it. Both null =
     * the platform-branded send.
     */
    void send(String to, String subject, String htmlBody,
              String senderDisplayName, String replyTo);
}
