package com.vointika.contact.application.usecase;

import com.vointika.contact.domain.entity.ContactMessage;
import com.vointika.contact.domain.repository.ContactMessageRepository;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContactMessageUseCasesTest {

    private static final UUID OP = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");
    private static final UUID USER = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID MSG = UUID.fromString("cccccccc-0000-4000-8000-000000000002");

    private ContactMessageRepository messageRepository;
    private TourOperatorMembershipCheck membershipCheck;
    private TransactionRunner transactionRunner;
    private AuditTrailPort auditTrailPort;

    @BeforeEach
    void setUp() {
        messageRepository = mock(ContactMessageRepository.class);
        membershipCheck = mock(TourOperatorMembershipCheck.class);
        transactionRunner = mock(TransactionRunner.class);
        auditTrailPort = mock(AuditTrailPort.class);
        doAnswer(i -> {
            ((Runnable) i.getArgument(0)).run();
            return null;
        }).when(transactionRunner).run(any());
        when(messageRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(messageRepository.findByIdAndTourOperatorId(MSG, OP))
                .thenReturn(Optional.of(unreadMessage()));
    }

    private ContactMessage unreadMessage() {
        return new ContactMessage(MSG, OP, "Laura Pérez", "laura@example.com",
                "Do you have child seats?", "We are a family of four…",
                null, Instant.parse("2026-07-28T10:00:00Z"));
    }

    private ContactMessage readMessage() {
        return new ContactMessage(MSG, OP, null, "ana@example.net",
                "Gift voucher?", "Do you sell gift vouchers?",
                Instant.parse("2026-07-28T11:00:00Z"), Instant.parse("2026-07-28T10:00:00Z"));
    }

    private SetContactMessageReadUseCase readUseCase() {
        return new SetContactMessageReadUseCase(messageRepository, membershipCheck,
                transactionRunner);
    }

    @Test
    void listRequiresMembership() {
        ListContactMessagesUseCase useCase =
                new ListContactMessagesUseCase(messageRepository, membershipCheck);
        ListQuery query = mock(ListQuery.class);
        when(query.tenantId()).thenReturn(OP);
        when(messageRepository.list(query))
                .thenReturn(new CursorPage<>(List.of(unreadMessage()), null));

        CursorPage<ContactMessage> page = useCase.execute(query, USER);

        verify(membershipCheck).ensureMember(USER, OP);
        assertThat(page.data()).hasSize(1);
    }

    @Test
    void getReturnsFullMessageToAnyMember() {
        GetContactMessageUseCase useCase =
                new GetContactMessageUseCase(messageRepository, membershipCheck);

        ContactMessage message = useCase.execute(OP, MSG, USER);

        verify(membershipCheck).ensureMember(USER, OP);
        assertThat(message.getContent()).contains("family of four");
    }

    @Test
    void getUnknownMessageIs404() {
        when(messageRepository.findByIdAndTourOperatorId(MSG, OP)).thenReturn(Optional.empty());
        GetContactMessageUseCase useCase =
                new GetContactMessageUseCase(messageRepository, membershipCheck);

        assertThatThrownBy(() -> useCase.execute(OP, MSG, USER))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void markReadStampsReadAtAndDoesNotAudit() {
        readUseCase().execute(OP, MSG, true, USER);

        verify(membershipCheck).ensureMember(USER, OP);
        ArgumentCaptor<ContactMessage> saved = ArgumentCaptor.forClass(ContactMessage.class);
        verify(messageRepository).save(saved.capture());
        assertThat(saved.getValue().isRead()).isTrue();
        assertThat(saved.getValue().getReadAt()).isNotNull();
        // Read-state is workflow bookkeeping — deliberately unaudited.
        verify(auditTrailPort, never()).append(any());
    }

    @Test
    void markReadTwiceKeepsFirstTimestamp() {
        ContactMessage alreadyRead = readMessage();
        Instant firstReadAt = alreadyRead.getReadAt();
        when(messageRepository.findByIdAndTourOperatorId(MSG, OP))
                .thenReturn(Optional.of(alreadyRead));

        readUseCase().execute(OP, MSG, true, USER);

        ArgumentCaptor<ContactMessage> saved = ArgumentCaptor.forClass(ContactMessage.class);
        verify(messageRepository).save(saved.capture());
        assertThat(saved.getValue().getReadAt()).isEqualTo(firstReadAt);
    }

    @Test
    void markUnreadClearsReadAt() {
        when(messageRepository.findByIdAndTourOperatorId(MSG, OP))
                .thenReturn(Optional.of(readMessage()));

        readUseCase().execute(OP, MSG, false, USER);

        ArgumentCaptor<ContactMessage> saved = ArgumentCaptor.forClass(ContactMessage.class);
        verify(messageRepository).save(saved.capture());
        assertThat(saved.getValue().isRead()).isFalse();
    }

    @Test
    void deleteIsAdminGatedAndAuditsSummaryOnly() {
        DeleteContactMessageUseCase useCase = new DeleteContactMessageUseCase(
                messageRepository, membershipCheck, transactionRunner, auditTrailPort);

        useCase.execute(OP, MSG, USER);

        verify(membershipCheck).ensureAdmin(USER, OP);
        verify(messageRepository).delete(MSG);
        ArgumentCaptor<NewAuditEntry> audit = ArgumentCaptor.forClass(NewAuditEntry.class);
        verify(auditTrailPort).append(audit.capture());
        assertThat(audit.getValue().action()).isEqualTo("contact_message.deleted");
        // The shopper's email must stay out of the trail.
        assertThat(audit.getValue().details()).containsOnlyKeys("summary");
    }

    @Test
    void deleteUnknownMessageIs404() {
        when(messageRepository.findByIdAndTourOperatorId(MSG, OP)).thenReturn(Optional.empty());
        DeleteContactMessageUseCase useCase = new DeleteContactMessageUseCase(
                messageRepository, membershipCheck, transactionRunner, auditTrailPort);

        assertThatThrownBy(() -> useCase.execute(OP, MSG, USER))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(messageRepository, never()).delete(any());
    }
}
