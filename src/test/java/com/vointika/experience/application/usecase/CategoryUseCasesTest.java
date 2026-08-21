package com.vointika.experience.application.usecase;

import com.vointika.experience.application.dto.input.CategoryInput;
import com.vointika.experience.domain.entity.Category;
import com.vointika.experience.domain.repository.CategoryRepository;
import com.vointika.experience.domain.valueobject.CategoryName;
import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceAlreadyExistsException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.exception.UniqueConstraintViolationException;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.service.IdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CategoryUseCasesTest {

    private static final UUID OPERATOR = UUID.fromString("019f8a41-2c55-7e02-9a17-3b6f0d4e8c31");
    private static final UUID CATEGORY = UUID.fromString("019f8a41-2c55-7e02-9a17-3b6f0d4e8c32");
    private static final UUID CALLER = UUID.fromString("019f8a41-2c55-7e02-9a17-3b6f0d4e8c33");

    private CategoryRepository repository;
    private TourOperatorMembershipCheck membershipCheck;
    private AuditTrailPort auditTrailPort;
    private TransactionRunner tx;

    @BeforeEach
    void setUp() {
        repository = mock(CategoryRepository.class);
        membershipCheck = mock(TourOperatorMembershipCheck.class);
        auditTrailPort = mock(AuditTrailPort.class);
        // The abstract finder is the stub; the default runs for real, so a 404
        // assertion below exercises the real branch instead of Mockito rethrowing
        // what it was told to throw (PATTERNS §9).
        doCallRealMethod().when(repository).requireByIdAndTourOperatorId(any(), any());
        when(repository.save(any())).thenAnswer(a -> a.getArgument(0));
        tx = new TransactionRunner() {
            @Override public <T> T call(Supplier<T> work) { return work.get(); }
            @Override public void run(Runnable work) { work.run(); }
        };
    }

    private Category existing(String name) {
        return new Category(CATEGORY, OPERATOR, new CategoryName(name),
                Instant.parse("2026-08-21T10:00:00Z"));
    }

    private CreateCategoryUseCase createUseCase() {
        IdGenerator idGenerator = mock(IdGenerator.class);
        when(idGenerator.newId()).thenReturn(CATEGORY);
        return new CreateCategoryUseCase(repository, membershipCheck, idGenerator, tx, auditTrailPort);
    }

    private UpdateCategoryUseCase updateUseCase() {
        return new UpdateCategoryUseCase(repository, membershipCheck, tx, auditTrailPort);
    }

    @Test
    void createStoresTheCategoryAndAuditsIt() {
        UUID id = createUseCase().execute(OPERATOR, CALLER, new CategoryInput("Boat tours"));

        assertThat(id).isEqualTo(CATEGORY);
        verify(membershipCheck).ensureAdmin(CALLER, OPERATOR);

        ArgumentCaptor<Category> saved = ArgumentCaptor.forClass(Category.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getName().value()).isEqualTo("Boat tours");
        assertThat(saved.getValue().getTourOperatorId()).isEqualTo(OPERATOR);

        ArgumentCaptor<NewAuditEntry> entry = ArgumentCaptor.forClass(NewAuditEntry.class);
        verify(auditTrailPort).append(entry.capture());
        assertThat(entry.getValue().action()).isEqualTo("category.created");
        assertThat(entry.getValue().entityType()).isEqualTo("CATEGORY");
    }

    @Test
    void aStaffMemberCannotCreate() {
        doThrow(new ForbiddenException(TourOperatorMembershipCheck.requiresRoleMessage("ADMIN")))
                .when(membershipCheck).ensureAdmin(CALLER, OPERATOR);

        assertThatThrownBy(() -> createUseCase().execute(OPERATOR, CALLER, new CategoryInput("Boat tours")))
                .isInstanceOf(ForbiddenException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void aBlankNameIs422BeforeAnythingIsWritten() {
        assertThatThrownBy(() -> createUseCase().execute(OPERATOR, CALLER, new CategoryInput("  ")))
                .isInstanceOf(InvalidFieldException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void aDuplicateNameIsRefusedByThePreCheck() {
        when(repository.existsByTourOperatorIdAndName(OPERATOR, "Boat tours")).thenReturn(true);

        assertThatThrownBy(() -> createUseCase().execute(OPERATOR, CALLER, new CategoryInput("Boat tours")))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessage(CategoryRepository.NAME_TAKEN);
        verify(repository, never()).save(any());
    }

    /**
     * The pre-check and the lost race must answer identically — otherwise a
     * concurrent create is a 500 where a sequential one is a clean 409
     * (PATTERNS §8d).
     */
    @Test
    void losingTheUniqueIndexRaceIsTheSame409AsThePreCheck() {
        when(repository.save(any())).thenThrow(new UniqueConstraintViolationException("dup", null));

        assertThatThrownBy(() -> createUseCase().execute(OPERATOR, CALLER, new CategoryInput("Boat tours")))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessage(CategoryRepository.NAME_TAKEN);
    }

    @Test
    void renameStoresTheNewNameAndDiffsIt() {
        when(repository.findByIdAndTourOperatorId(CATEGORY, OPERATOR))
                .thenReturn(Optional.of(existing("Boat tours")));

        updateUseCase().execute(OPERATOR, CATEGORY, CALLER, new CategoryInput("Boat trips"));

        ArgumentCaptor<Category> saved = ArgumentCaptor.forClass(Category.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getName().value()).isEqualTo("Boat trips");

        ArgumentCaptor<NewAuditEntry> entry = ArgumentCaptor.forClass(NewAuditEntry.class);
        verify(auditTrailPort).append(entry.capture());
        assertThat(entry.getValue().action()).isEqualTo("category.updated");
        assertThat(entry.getValue().changes())
                .singleElement()
                .satisfies(c -> assertThat(c.field()).isEqualTo("name"));
    }

    /** An absent name is the whole payload here, so the call has nothing to do. */
    @Test
    void anAbsentNameWritesNothing() {
        when(repository.findByIdAndTourOperatorId(CATEGORY, OPERATOR))
                .thenReturn(Optional.of(existing("Boat tours")));

        updateUseCase().execute(OPERATOR, CATEGORY, CALLER, new CategoryInput(null));

        verify(repository, never()).save(any());
        verify(auditTrailPort, never()).append(any());
    }

    @Test
    void renamingToTheSameValueWritesNothing() {
        when(repository.findByIdAndTourOperatorId(CATEGORY, OPERATOR))
                .thenReturn(Optional.of(existing("Boat tours")));

        updateUseCase().execute(OPERATOR, CATEGORY, CALLER, new CategoryInput("Boat tours"));

        verify(repository, never()).save(any());
        verify(auditTrailPort, never()).append(any());
    }

    /**
     * A case-only edit is a real rename, not a self-conflict: the clash check
     * excludes this category, so "boat tours" → "Boat Tours" lands.
     */
    @Test
    void aCaseOnlyRenameIsPersisted() {
        when(repository.findByIdAndTourOperatorId(CATEGORY, OPERATOR))
                .thenReturn(Optional.of(existing("boat tours")));

        updateUseCase().execute(OPERATOR, CATEGORY, CALLER, new CategoryInput("Boat Tours"));

        ArgumentCaptor<Category> saved = ArgumentCaptor.forClass(Category.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getName().value()).isEqualTo("Boat Tours");
    }

    @Test
    void renamingOntoAnotherCategorysNameIs409() {
        when(repository.findByIdAndTourOperatorId(CATEGORY, OPERATOR))
                .thenReturn(Optional.of(existing("Boat tours")));
        when(repository.existsByTourOperatorIdAndNameExcluding(OPERATOR, "Walking tours", CATEGORY))
                .thenReturn(true);

        assertThatThrownBy(() -> updateUseCase()
                .execute(OPERATOR, CATEGORY, CALLER, new CategoryInput("Walking tours")))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessage(CategoryRepository.NAME_TAKEN);
        verify(repository, never()).save(any());
    }

    @Test
    void updatingAnUnknownCategoryIs404() {
        when(repository.findByIdAndTourOperatorId(CATEGORY, OPERATOR)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> updateUseCase()
                .execute(OPERATOR, CATEGORY, CALLER, new CategoryInput("Boat trips")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(CategoryRepository.NOT_FOUND);
    }

    @Test
    void getIsMemberVisible() {
        Category category = existing("Boat tours");
        when(repository.findByIdAndTourOperatorId(CATEGORY, OPERATOR)).thenReturn(Optional.of(category));

        Category found = new GetCategoryUseCase(repository, membershipCheck)
                .execute(OPERATOR, CATEGORY, CALLER);

        assertThat(found).isSameAs(category);
        verify(membershipCheck).ensureMember(CALLER, OPERATOR);
    }

    @Test
    void listIsMemberVisibleAndTenantScoped() {
        ListQuery query = mock(ListQuery.class);
        when(query.tenantId()).thenReturn(OPERATOR);
        CursorPage<Category> page = new CursorPage<>(List.of(existing("Boat tours")), null);
        when(repository.list(query)).thenReturn(page);

        CursorPage<Category> result =
                new ListCategoriesUseCase(repository, membershipCheck).execute(query, CALLER);

        assertThat(result).isSameAs(page);
        verify(membershipCheck).ensureMember(CALLER, OPERATOR);
    }

    /**
     * The experiences filed under it are the database's problem —
     * {@code ON DELETE SET NULL} — so the use case neither refuses nor reassigns.
     * The name rides the audit entry because the row is gone afterwards.
     */
    @Test
    void deleteRemovesTheCategoryAndAuditsItsName() {
        Category category = existing("Boat tours");
        when(repository.findByIdAndTourOperatorId(CATEGORY, OPERATOR)).thenReturn(Optional.of(category));

        new DeleteCategoryUseCase(repository, membershipCheck, tx, auditTrailPort)
                .execute(OPERATOR, CATEGORY, CALLER);

        verify(membershipCheck).ensureAdmin(CALLER, OPERATOR);
        verify(repository).delete(category);

        ArgumentCaptor<NewAuditEntry> entry = ArgumentCaptor.forClass(NewAuditEntry.class);
        verify(auditTrailPort).append(entry.capture());
        assertThat(entry.getValue().action()).isEqualTo("category.deleted");
        assertThat(entry.getValue().details()).containsEntry("name", "Boat tours");
    }

    @Test
    void deletingAnUnknownCategoryIs404AndRemovesNothing() {
        when(repository.findByIdAndTourOperatorId(CATEGORY, OPERATOR)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new DeleteCategoryUseCase(repository, membershipCheck, tx, auditTrailPort)
                .execute(OPERATOR, CATEGORY, CALLER))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(CategoryRepository.NOT_FOUND);
        verify(repository, never()).delete(any());
    }

    @Test
    void aStaffMemberCannotDelete() {
        doThrow(new ForbiddenException(TourOperatorMembershipCheck.requiresRoleMessage("ADMIN")))
                .when(membershipCheck).ensureAdmin(CALLER, OPERATOR);

        assertThatThrownBy(() -> new DeleteCategoryUseCase(repository, membershipCheck, tx, auditTrailPort)
                .execute(OPERATOR, CATEGORY, CALLER))
                .isInstanceOf(ForbiddenException.class);
        verify(repository, never()).delete(any());
    }

    @Test
    void anEightyCharacterNameIsAccepted() {
        assertThatCode(() -> createUseCase()
                .execute(OPERATOR, CALLER, new CategoryInput("x".repeat(80))))
                .doesNotThrowAnyException();
    }
}
