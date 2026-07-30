package com.vointika.contact.application.usecase;

import com.vointika.contact.domain.entity.ContactMessage;
import com.vointika.contact.domain.repository.ContactMessageRepository;
import com.vointika.contact.domain.valueobject.ContactContent;
import com.vointika.contact.domain.valueobject.ContactEmail;
import com.vointika.contact.domain.valueobject.ContactName;
import com.vointika.contact.domain.valueobject.ContactSummary;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.exception.TooManyRequestsException;

import com.vointika.shared.port.RateLimiterPort;
import com.vointika.shared.port.StorefrontOperatorQuery;
import com.vointika.shared.port.StorefrontOperatorView;
import com.vointika.shared.service.IdGenerator;

import java.time.Duration;
import java.time.Instant;

/**
 * A shopper writes to an operator — the inbox's first and only writer.
 *
 * <p>Called from the storefront BFF over the internal API, on behalf of someone
 * with no account and no session. Everything that follows from that: the tenant
 * is addressed by slug, the input is validated by value objects rather than
 * trusted, and there is a throttle.
 *
 * <p><strong>Not audited, deliberately.</strong> The audit trail records what
 * operator-facing actors did; this is inbound, and the message row is already a
 * complete, permanent record of it. An audit entry would duplicate the row it
 * describes. (That is also why the {@code STOREFRONT} actor is still not needed:
 * it lands with the first storefront write that is not self-recording.)
 */
public class SubmitContactMessageUseCase {

    /**
     * A per-tenant backstop, not per-visitor.
     *
     * <p>Per-IP belongs at the edge, where the Worker has the visitor's real
     * address from Cloudflare for free. Here the caller is always the Worker, so
     * an IP would have to be forwarded and trusted — a header any holder of the
     * shared secret could set. Limiting per tenant needs no such trust and still
     * bounds what one storefront's form can do to the database.
     */
    private static final int MAX_PER_WINDOW = 30;
    private static final Duration WINDOW = Duration.ofHours(1);

    private final ContactMessageRepository messageRepository;
    private final StorefrontOperatorQuery storefrontOperatorQuery;
    private final RateLimiterPort rateLimiter;
    private final IdGenerator idGenerator;

    public SubmitContactMessageUseCase(ContactMessageRepository messageRepository,
                                       StorefrontOperatorQuery storefrontOperatorQuery,
                                       RateLimiterPort rateLimiter,
                                       IdGenerator idGenerator) {
        this.messageRepository = messageRepository;
        this.storefrontOperatorQuery = storefrontOperatorQuery;
        this.rateLimiter = rateLimiter;
        this.idGenerator = idGenerator;
    }

    public void execute(String tenantSlug, String name, String email, String summary, String content) {
        StorefrontOperatorView operator = storefrontOperatorQuery.findBySlug(tenantSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Storefront not found"));

        // Throttle before validating: a flood should cost as little as possible,
        // and a rejected message must not be cheaper to send than a real one.
        if (!rateLimiter.tryAcquire("rl:contact:tenant:" + operator.id(), MAX_PER_WINDOW, WINDOW)) {
            throw new TooManyRequestsException("Too many messages, please try again later");
        }

        messageRepository.save(ContactMessage.submit(
                idGenerator.newId(),
                operator.id(),
                new ContactName(name),
                new ContactEmail(email),
                new ContactSummary(summary),
                new ContactContent(content),
                Instant.now()));
    }
}
