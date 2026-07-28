package com.vointika.contact.application.usecase;

import com.vointika.contact.domain.entity.ContactMessage;
import com.vointika.contact.domain.repository.ContactMessageRepository;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;

import java.util.UUID;

/**
 * Flips the inbox read-state. Any member (STAFF answers inquiries), and
 * idempotent — re-reading keeps the first read timestamp, re-unreading is a
 * no-op. UNAUDITED BY DESIGN: read-state is workflow bookkeeping, not a
 * business mutation (the per-item-menu-diff reasoning); delete IS audited.
 */
public class SetContactMessageReadUseCase {

    private final ContactMessageRepository messageRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;

    public SetContactMessageReadUseCase(ContactMessageRepository messageRepository,
                                        TourOperatorMembershipCheck membershipCheck,
                                        TransactionRunner transactionRunner) {
        this.messageRepository = messageRepository;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
    }

    public void execute(UUID tourOperatorId, UUID messageId, boolean read, UUID callerUserId) {
        membershipCheck.ensureMember(callerUserId, tourOperatorId);
        ContactMessage message = messageRepository
                .findByIdAndTourOperatorId(messageId, tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact message not found"));

        if (read) {
            message.markRead();
        } else {
            message.markUnread();
        }
        transactionRunner.run(() -> messageRepository.save(message));
    }
}
