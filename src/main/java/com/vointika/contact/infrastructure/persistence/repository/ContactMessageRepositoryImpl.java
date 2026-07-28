package com.vointika.contact.infrastructure.persistence.repository;

import com.vointika.contact.application.usecase.ListContactMessagesUseCase;
import com.vointika.contact.domain.entity.ContactMessage;
import com.vointika.contact.domain.repository.ContactMessageRepository;
import com.vointika.contact.infrastructure.persistence.entity.ContactMessageJpaEntity;
import com.vointika.contact.infrastructure.persistence.mapper.ContactMessageMapper;
import com.vointika.shared.infrastructure.list.CriteriaListExecutor;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class ContactMessageRepositoryImpl implements ContactMessageRepository {

    private final ContactMessageJpaRepository messageJpa;
    private final CriteriaListExecutor listExecutor;

    public ContactMessageRepositoryImpl(ContactMessageJpaRepository messageJpa,
                                        CriteriaListExecutor listExecutor) {
        this.messageJpa = messageJpa;
        this.listExecutor = listExecutor;
    }

    @Override
    public ContactMessage save(ContactMessage message) {
        return ContactMessageMapper.toDomain(messageJpa.save(ContactMessageMapper.toJpa(message)));
    }

    @Override
    public Optional<ContactMessage> findByIdAndTourOperatorId(UUID messageId, UUID tourOperatorId) {
        return messageJpa.findByIdAndTourOperatorId(messageId, tourOperatorId)
                .map(ContactMessageMapper::toDomain);
    }

    @Override
    public CursorPage<ContactMessage> list(ListQuery query) {
        return listExecutor.list(
                ContactMessageJpaEntity.class,
                ListContactMessagesUseCase.SCHEMA,
                query,
                ContactMessageMapper::toDomain);
    }

    @Override
    public void delete(UUID messageId) {
        messageJpa.deleteById(messageId);
    }
}
