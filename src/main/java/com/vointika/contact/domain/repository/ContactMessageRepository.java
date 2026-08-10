package com.vointika.contact.domain.repository;

import com.vointika.contact.domain.entity.ContactMessage;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;

import java.util.Optional;
import java.util.UUID;

public interface ContactMessageRepository {

    Optional<ContactMessage> findByIdAndTourOperatorId(UUID messageId, UUID tourOperatorId);

    CursorPage<ContactMessage> list(ListQuery query);

    void delete(UUID messageId);
}
