package com.vointika.page.application.usecase;

import com.vointika.page.application.dto.input.CreatePageInput;
import com.vointika.page.application.dto.input.UpdatePageInput;
import com.vointika.page.domain.entity.Page;
import com.vointika.page.domain.repository.PageRepository;
import com.vointika.page.domain.repository.PageTranslationRepository;
import com.vointika.page.domain.valueobject.PageBody;
import com.vointika.page.domain.valueobject.PageTitle;
import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.exception.ResourceAlreadyExistsException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.service.IdGenerator;
import com.vointika.shared.valueobject.Slug;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PageUseCasesTest {

    private static final UUID OP = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");
    private static final UUID USER = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID PAGE = UUID.fromString("bbbbbbbb-0000-4000-8000-000000000001");

    private PageRepository repository;
    private PageTranslationRepository translationRepository;
    private TourOperatorMembershipCheck membershipCheck;
    private TransactionRunner transactionRunner;
    private IdGenerator idGenerator;
    private AuditTrailPort auditTrailPort;

    @BeforeEach
    void setUp() {
        repository = mock(PageRepository.class);
        translationRepository = mock(PageTranslationRepository.class);
        membershipCheck = mock(TourOperatorMembershipCheck.class);
        transactionRunner = mock(TransactionRunner.class);
        idGenerator = mock(IdGenerator.class);
        auditTrailPort = mock(AuditTrailPort.class);
        when(transactionRunner.call(any())).thenAnswer(i -> ((Supplier<?>) i.getArgument(0)).get());
        doAnswer(i -> {
            ((Runnable) i.getArgument(0)).run();
            return null;
        }).when(transactionRunner).run(any());
        when(idGenerator.newId()).thenReturn(PAGE);
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private Page page() {
        return new Page(PAGE, OP, new PageTitle("About us"), new Slug("about-us"),
                new PageBody("<p>Hello</p>"), null, null, USER);
    }

    private CreatePageInput createInput(String handle) {
        return new CreatePageInput(USER, OP, "About us", handle, "<p>Hello</p>", null, null);
    }

    // ---- create ----

    @Test
    void createPersistsDraftAndAudits() {
        when(repository.existsByTourOperatorIdAndHandle(OP, "about-us")).thenReturn(false);

        UUID id = create().execute(createInput("about-us"));

        assertThat(id).isEqualTo(PAGE);
        verify(membershipCheck).ensureAdmin(USER, OP);
        verify(repository).save(any());
        verify(auditTrailPort).append(any());
    }

    @Test
    void createRejectsDuplicateHandle() {
        when(repository.existsByTourOperatorIdAndHandle(OP, "about-us")).thenReturn(true);

        assertThatThrownBy(() -> create().execute(createInput("about-us")))
                .isInstanceOf(ResourceAlreadyExistsException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void createRequiresAdmin() {
        doThrow(new ForbiddenException("admin")).when(membershipCheck).ensureAdmin(USER, OP);
        assertThatThrownBy(() -> create().execute(createInput("about-us")))
                .isInstanceOf(ForbiddenException.class);
    }

    // ---- update ----

    @Test
    void updateReplacesContentAndAuditsTheDiff() {
        when(repository.findByIdAndTourOperatorId(PAGE, OP)).thenReturn(Optional.of(page()));

        update().execute(new UpdatePageInput(USER, OP, PAGE,
                "New title", "<p>Hello</p>", "SEO", null, null));

        verify(repository).save(any());
        verify(auditTrailPort).append(any());
    }

    @Test
    void noOpUpdateSavesButRecordsNothing() {
        when(repository.findByIdAndTourOperatorId(PAGE, OP)).thenReturn(Optional.of(page()));

        update().execute(new UpdatePageInput(USER, OP, PAGE,
                "About us", "<p>Hello</p>", null, null, null));

        verify(repository).save(any());
        verify(auditTrailPort, never()).append(any());
    }

    @Test
    void updateMissingPageIs404() {
        when(repository.findByIdAndTourOperatorId(PAGE, OP)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> update().execute(new UpdatePageInput(USER, OP, PAGE,
                "T", "<p>B</p>", null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- publish / unpublish ----

    @Test
    void publishFlipsStatusAndAudits() {
        when(repository.findByIdAndTourOperatorId(PAGE, OP)).thenReturn(Optional.of(page()));

        new PublishPageUseCase(repository, membershipCheck, transactionRunner, auditTrailPort)
                .execute(OP, PAGE, USER);

        verify(repository).save(any());
        verify(auditTrailPort).append(any());
    }

    // ---- rename ----

    @Test
    void renameToTheSameHandleIsANoOp() {
        when(repository.findByIdAndTourOperatorId(PAGE, OP)).thenReturn(Optional.of(page()));

        rename().execute(OP, PAGE, "about-us", USER);

        verify(repository, never()).save(any());
        verify(auditTrailPort, never()).append(any());
    }

    @Test
    void renameRejectsATakenHandle() {
        when(repository.findByIdAndTourOperatorId(PAGE, OP)).thenReturn(Optional.of(page()));
        when(repository.existsByTourOperatorIdAndHandle(OP, "about")).thenReturn(true);

        assertThatThrownBy(() -> rename().execute(OP, PAGE, "about", USER))
                .isInstanceOf(ResourceAlreadyExistsException.class);
    }

    @Test
    void renameSavesAndAuditsTheHandleDiff() {
        when(repository.findByIdAndTourOperatorId(PAGE, OP)).thenReturn(Optional.of(page()));
        when(repository.existsByTourOperatorIdAndHandle(OP, "about")).thenReturn(false);

        rename().execute(OP, PAGE, "about", USER);

        verify(repository).save(any());
        verify(auditTrailPort).append(any());
    }

    // ---- delete ----

    @Test
    void deleteRemovesAndRecordsTheIdentity() {
        when(repository.findByIdAndTourOperatorId(PAGE, OP)).thenReturn(Optional.of(page()));

        new DeletePageUseCase(repository, membershipCheck, transactionRunner, auditTrailPort)
                .execute(OP, PAGE, USER);

        verify(membershipCheck).ensureAdmin(USER, OP);
        verify(repository).delete(PAGE);
        verify(auditTrailPort).append(any());
    }

    @Test
    void createRejectsAHandleAnotherPageAlreadyUsesAsALocalizedHandle() {
        when(repository.existsByTourOperatorIdAndHandle(OP, "sobre-nosotros")).thenReturn(false);
        when(translationRepository.existsBySlugInAnyLocale(OP, "sobre-nosotros", null)).thenReturn(true);

        assertThatThrownBy(() -> create().execute(new CreatePageInput(
                USER, OP, "Sobre nosotros", "sobre-nosotros", "<p>x</p>", null, null)))
                .isInstanceOf(ResourceAlreadyExistsException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void renameRejectsAHandleAnotherPageAlreadyUsesAsALocalizedHandle() {
        when(repository.findByIdAndTourOperatorId(PAGE, OP)).thenReturn(Optional.of(page()));
        when(repository.existsByTourOperatorIdAndHandle(OP, "sobre-nosotros")).thenReturn(false);
        when(translationRepository.existsBySlugInAnyLocale(OP, "sobre-nosotros", PAGE)).thenReturn(true);

        assertThatThrownBy(() -> rename().execute(OP, PAGE, "sobre-nosotros", USER))
                .isInstanceOf(ResourceAlreadyExistsException.class);

        verify(repository, never()).save(any());
    }

    private CreatePageUseCase create() {
        return new CreatePageUseCase(repository, translationRepository, membershipCheck,
                idGenerator, transactionRunner, auditTrailPort);
    }

    private UpdatePageUseCase update() {
        return new UpdatePageUseCase(repository, membershipCheck, transactionRunner, auditTrailPort);
    }

    private RenamePageUseCase rename() {
        return new RenamePageUseCase(repository, translationRepository, membershipCheck,
                transactionRunner, auditTrailPort);
    }
}
