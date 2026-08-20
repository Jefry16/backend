package com.vointika.contact.application.usecase;

import com.vointika.contact.domain.entity.ContactMessage;
import com.vointika.contact.domain.repository.ContactMessageRepository;
import com.vointika.shared.port.TourOperatorMembershipCheck;

import java.util.UUID;

/** One message, full content. Any member; cross-tenant → 404. */
public class GetContactMessageUseCase {

    private final ContactMessageRepository messageRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public GetContactMessageUseCase(ContactMessageRepository messageRepository,
                                    TourOperatorMembershipCheck membershipCheck) {
        this.messageRepository = messageRepository;
        this.membershipCheck = membershipCheck;
    }

    public ContactMessage execute(UUID tourOperatorId, UUID messageId, UUID callerUserId) {
        membershipCheck.ensureMember(callerUserId, tourOperatorId);
        return messageRepository.requireByIdAndTourOperatorId(messageId, tourOperatorId);
    }
}
