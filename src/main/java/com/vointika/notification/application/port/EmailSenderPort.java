package com.vointika.notification.application.port;

public interface EmailSenderPort {

    /**
     * The one send. {@code senderDisplayName}
     * (nullable) becomes the From header's display name over the platform
     * from-address ({@code "Name <from>"}); the adapter must sanitize it
     * against header injection (strip CR/LF and {@code <>}). {@code replyTo}
     * (nullable) sets the Reply-To header; {@code null} omits it. Both null is the
     * platform-branded send that every identity and team email uses.
     */
    void send(String to, String subject, String htmlBody,
              String senderDisplayName, String replyTo);
}
