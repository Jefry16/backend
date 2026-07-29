package com.vointika.contact.application.usecase;

import com.vointika.contact.domain.entity.ContactMessage;
import com.vointika.contact.domain.repository.ContactMessageRepository;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.AuditActor;

import java.util.Map;
import java.util.UUID;

/**
 * Hard-deletes a message (spam, handled inquiries). ADMIN+ only. Audited
 * with the summary only — the shopper's email stays out of the trail (the
 * row was the record; once deleted, the trail shouldn't preserve PII the
 * delete was meant to remove).
 */
public class DeleteContactMessageUseCase {

    private final ContactMessageRepository messageRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public DeleteContactMessageUseCase(ContactMessageRepository messageRepository,
                                       TourOperatorMembershipCheck membershipCheck,
                                       TransactionRunner transactionRunner,
                                       AuditTrailPort auditTrailPort) {
        this.messageRepository = messageRepository;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(UUID tourOperatorId, UUID messageId, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        ContactMessage message = messageRepository
                .findByIdAndTourOperatorId(messageId, tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact message not found"));

        transactionRunner.run(() -> {
            messageRepository.delete(message.getId());
            auditTrailPort.append(new NewAuditEntry(
                    tourOperatorId, AuditActor.user(callerUserId),
                    "CONTACT_MESSAGE", message.getId(), "contact_message.deleted",
                    Map.of("summary", message.getSummary())));
        });
    }
}
