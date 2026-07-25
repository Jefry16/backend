package com.vointika.audience.application.usecase;

import com.vointika.audience.application.dto.input.AudienceInput;
import com.vointika.audience.domain.entity.Audience;
import com.vointika.audience.domain.repository.AudienceRepository;
import com.vointika.audience.domain.valueobject.AudienceName;
import com.vointika.audience.domain.valueobject.PaxPerUnit;
import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.exception.ResourceAlreadyExistsException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.service.IdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AudienceUseCasesTest {

    private static final UUID OP = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");
    private static final UUID USER = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID AUD = UUID.fromString("aaaaaaaa-0000-4000-8000-000000000001");

    private AudienceRepository repository;
    private TourOperatorMembershipCheck membershipCheck;
    private IdGenerator idGenerator;
    private TransactionRunner transactionRunner;

    @BeforeEach
    void setUp() {
        repository = mock(AudienceRepository.class);
        membershipCheck = mock(TourOperatorMembershipCheck.class);
        idGenerator = mock(IdGenerator.class);
        transactionRunner = mock(TransactionRunner.class);
        when(transactionRunner.call(any())).thenAnswer(i -> ((Supplier<?>) i.getArgument(0)).get());
        doAnswer(i -> {
            ((Runnable) i.getArgument(0)).run();
            return null;
        }).when(transactionRunner).run(any());
    }

    private Audience audience(String name, int pax) {
        return new Audience(AUD, OP, new AudienceName(name), new PaxPerUnit(pax), USER, Instant.now());
    }

    // ---- create ----

    @Test
    void createPersistsAndReturnsId() {
        when(idGenerator.newId()).thenReturn(AUD);
        when(repository.existsByTourOperatorIdAndName(OP, "Adults")).thenReturn(false);
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        UUID id = new CreateAudienceUseCase(repository, membershipCheck, idGenerator, transactionRunner)
                .execute(OP, USER, new AudienceInput("Adults", 1));

        assertThat(id).isEqualTo(AUD);
        verify(membershipCheck).ensureAdmin(USER, OP);
        verify(repository).save(any());
    }

    @Test
    void createRejectsDuplicateNameUpFront() {
        when(repository.existsByTourOperatorIdAndName(OP, "Adults")).thenReturn(true);

        assertThatThrownBy(() -> new CreateAudienceUseCase(repository, membershipCheck, idGenerator, transactionRunner)
                .execute(OP, USER, new AudienceInput("Adults", 1)))
                .isInstanceOf(ResourceAlreadyExistsException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void createRequiresAdmin() {
        doThrow(new ForbiddenException("admin")).when(membershipCheck).ensureAdmin(USER, OP);

        assertThatThrownBy(() -> new CreateAudienceUseCase(repository, membershipCheck, idGenerator, transactionRunner)
                .execute(OP, USER, new AudienceInput("Adults", 1)))
                .isInstanceOf(ForbiddenException.class);
        verify(repository, never()).save(any());
    }

    // ---- update ----

    @Test
    void updateRenamesAndSaves() {
        when(repository.findByIdAndTourOperatorId(AUD, OP)).thenReturn(Optional.of(audience("Adults", 1)));
        when(repository.existsByTourOperatorIdAndNameExcluding(eq(OP), anyString(), eq(AUD))).thenReturn(false);

        new UpdateAudienceUseCase(repository, membershipCheck, transactionRunner)
                .execute(OP, AUD, USER, new AudienceInput("Seniors", 1));

        verify(membershipCheck).ensureAdmin(USER, OP);
        verify(repository).save(any());
    }

    @Test
    void updateMissingIs404() {
        when(repository.findByIdAndTourOperatorId(AUD, OP)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new UpdateAudienceUseCase(repository, membershipCheck, transactionRunner)
                .execute(OP, AUD, USER, new AudienceInput("Seniors", 1)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateRejectsDuplicateName() {
        when(repository.findByIdAndTourOperatorId(AUD, OP)).thenReturn(Optional.of(audience("Adults", 1)));
        when(repository.existsByTourOperatorIdAndNameExcluding(eq(OP), eq("Seniors"), eq(AUD))).thenReturn(true);

        assertThatThrownBy(() -> new UpdateAudienceUseCase(repository, membershipCheck, transactionRunner)
                .execute(OP, AUD, USER, new AudienceInput("Seniors", 1)))
                .isInstanceOf(ResourceAlreadyExistsException.class);
        verify(repository, never()).save(any());
    }

    // ---- get / delete ----

    @Test
    void getRequiresMemberAndReturns() {
        when(repository.findByIdAndTourOperatorId(AUD, OP)).thenReturn(Optional.of(audience("Adults", 1)));

        Audience result = new GetAudienceUseCase(repository, membershipCheck).execute(OP, AUD, USER);

        assertThat(result.getName().value()).isEqualTo("Adults");
        verify(membershipCheck).ensureMember(USER, OP);
    }

    @Test
    void getMissingIs404() {
        when(repository.findByIdAndTourOperatorId(AUD, OP)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new GetAudienceUseCase(repository, membershipCheck).execute(OP, AUD, USER))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteRequiresAdminAndRemoves() {
        when(repository.findByIdAndTourOperatorId(AUD, OP)).thenReturn(Optional.of(audience("Adults", 1)));

        new DeleteAudienceUseCase(repository, membershipCheck).execute(OP, AUD, USER);

        verify(membershipCheck).ensureAdmin(USER, OP);
        verify(repository).deleteById(AUD);
    }

    @Test
    void deleteMissingIs404() {
        when(repository.findByIdAndTourOperatorId(AUD, OP)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new DeleteAudienceUseCase(repository, membershipCheck).execute(OP, AUD, USER))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).deleteById(any());
    }
}
