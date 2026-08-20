package com.vointika.contact.domain.repository;

import com.vointika.contact.domain.entity.ContactMessage;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;

import java.util.Optional;
import java.util.UUID;

public interface ContactMessageRepository {

    /**
     * The 404 for a message id that resolves to nothing this operator owns.
     *
     * <p>On the repository, not a shared port: nothing outside {@code contact} reaches a
     * contact message. It exists as a constant chiefly so the two documentation tests can
     * <b>derive</b> the body they publish — they spelled it out, which meant rewording the
     * throw moved nothing in the guide (PATTERNS §9a).
     */
    String NOT_FOUND = "Contact message not found";

    Optional<ContactMessage> findByIdAndTourOperatorId(UUID messageId, UUID tourOperatorId);

    /** The tenant-scoped read. Both callers use the row, so there is no requireExists. */
    default ContactMessage requireByIdAndTourOperatorId(UUID messageId, UUID tourOperatorId) {
        return findByIdAndTourOperatorId(messageId, tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND));
    }

    CursorPage<ContactMessage> list(ListQuery query);

    void delete(UUID messageId);
}
