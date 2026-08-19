package com.vointika.audience.application.usecase;

import com.vointika.shared.service.OperatorLocaleCheck;
import com.vointika.audience.domain.entity.Audience;
import com.vointika.audience.domain.entity.AudienceTranslation;
import com.vointika.audience.domain.repository.AudienceRepository;
import com.vointika.audience.domain.repository.AudienceTranslationRepository;
import com.vointika.audience.domain.valueobject.AudienceName;
import com.vointika.audience.domain.valueobject.PaxPerUnit;
import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.OperatorLocalesQuery;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.LocaleCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AudienceTranslationUseCasesTest {

    private static final UUID OP = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");
    private static final UUID USER = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID AUD = UUID.fromString("aaaaaaaa-0000-4000-8000-000000000001");

    private AudienceRepository audienceRepository;
    private AudienceTranslationRepository translationRepository;
    private OperatorLocalesQuery operatorLocalesQuery;
    private TourOperatorMembershipCheck membershipCheck;
    private TransactionRunner transactionRunner;
    private AuditTrailPort auditTrailPort;

    @BeforeEach
    void setUp() {
        audienceRepository = mock(AudienceRepository.class);
        // Both are default methods, and requireExists delegates to the other, so
        // both need calling for real — otherwise Mockito returns null from each and
        // every 404 assertion below passes without running the branch (PATTERNS §9).
        doCallRealMethod().when(audienceRepository).requireByIdAndTourOperatorId(any(), any());
        doCallRealMethod().when(audienceRepository).requireExists(any(), any());
        translationRepository = mock(AudienceTranslationRepository.class);
        operatorLocalesQuery = mock(OperatorLocalesQuery.class);
        membershipCheck = mock(TourOperatorMembershipCheck.class);
        transactionRunner = mock(TransactionRunner.class);
        auditTrailPort = mock(AuditTrailPort.class);
        doAnswer(i -> {
            ((Runnable) i.getArgument(0)).run();
            return null;
        }).when(transactionRunner).run(any());
        // The upsert/delete no-op probe: default = no existing overlay.
        when(translationRepository.findByAudienceIdAndLocale(any(), any()))
                .thenReturn(Optional.empty());
        when(audienceRepository.findByIdAndTourOperatorId(AUD, OP)).thenReturn(Optional.of(
                new Audience(AUD, OP, new AudienceName("Adults"), new PaxPerUnit(1), USER, Instant.now())));
        when(operatorLocalesQuery.findSupportedLocales(OP)).thenReturn(Set.of("en", "es"));
    }

    private UpsertAudienceTranslationUseCase upsert() {
        return new UpsertAudienceTranslationUseCase(
                audienceRepository, translationRepository, new OperatorLocaleCheck(operatorLocalesQuery), membershipCheck,
                transactionRunner, auditTrailPort);
    }

    private DeleteAudienceTranslationUseCase delete() {
        return new DeleteAudienceTranslationUseCase(
                audienceRepository, translationRepository, membershipCheck,
                transactionRunner, auditTrailPort);
    }

    private AudienceTranslation overlay(String name) {
        return new AudienceTranslation(AUD, OP, new LocaleCode("es"),
                name == null ? null : new AudienceName(name));
    }

    @Test
    void upsertStoresTranslatedName() {
        upsert().execute(OP, AUD, "es", "Adultos", USER);

        verify(membershipCheck).ensureAdmin(USER, OP);
        ArgumentCaptor<AudienceTranslation> captor = ArgumentCaptor.forClass(AudienceTranslation.class);
        verify(translationRepository).upsert(captor.capture());
        assertThat(captor.getValue().name().value()).isEqualTo("Adultos");
        assertThat(captor.getValue().locale().value()).isEqualTo("es");
    }

    @Test
    void upsertBlankNameDeletesTheOverlayInsteadOfStoringAnEmptyOne() {
        // Name is the only translatable column, so a row without it falls back
        // for everything — indistinguishable from no row, except that it shows
        // up in the translations list as a locale someone has worked on.
        when(translationRepository.findByAudienceIdAndLocale(AUD, "es"))
                .thenReturn(Optional.of(overlay("Adultos")));

        upsert().execute(OP, AUD, "es", "  ", USER);

        verify(translationRepository).deleteByAudienceIdAndLocale(AUD, "es");
        verify(translationRepository, never()).upsert(any());

        ArgumentCaptor<NewAuditEntry> audit = ArgumentCaptor.forClass(NewAuditEntry.class);
        verify(auditTrailPort).append(audit.capture());
        assertThat(audit.getValue().action()).isEqualTo("audience.translation_deleted");
        assertThat(audit.getValue().details()).containsEntry("locale", "es");
    }

    @Test
    void upsertBlankNameWhenThereWasNoOverlayWritesNothingAndAuditsNothing() {
        // The same-value probe catches nothing-to-nothing before any write.
        upsert().execute(OP, AUD, "es", "   ", USER);

        verify(translationRepository, never()).upsert(any());
        verify(translationRepository, never()).deleteByAudienceIdAndLocale(any(), any());
        verify(auditTrailPort, never()).append(any());
    }

    @Test
    void anEmptyTranslationKnowsItIsEmpty() {
        // empty() and isEmpty() are two halves of one idea; if a translatable
        // field is added to the record and not to isEmpty(), this fails.
        assertThat(AudienceTranslation.empty(AUD, OP, new LocaleCode("es")).isEmpty()).isTrue();
        assertThat(overlay("Adultos").isEmpty()).isFalse();
    }

    @Test
    void upsertSameValueIsNoOpAndRecordsNothing() {
        when(translationRepository.findByAudienceIdAndLocale(AUD, "es"))
                .thenReturn(Optional.of(overlay("Adultos")));

        upsert().execute(OP, AUD, "es", "Adultos", USER);

        verify(translationRepository, never()).upsert(any());
        verify(auditTrailPort, never()).append(any());
    }

    @Test
    void upsertUnsupportedLocaleIs422() {
        assertThatThrownBy(() -> upsert().execute(OP, AUD, "fr", "Adultes", USER))
                .isInstanceOf(InvalidFieldException.class);
        verify(translationRepository, never()).upsert(any());
    }

    @Test
    void upsertMissingAudienceIs404() {
        when(audienceRepository.findByIdAndTourOperatorId(AUD, OP)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> upsert().execute(OP, AUD, "es", "Adultos", USER))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void upsertRequiresAdmin() {
        doThrow(new ForbiddenException("admin")).when(membershipCheck).ensureAdmin(USER, OP);
        assertThatThrownBy(() -> upsert().execute(OP, AUD, "es", "Adultos", USER))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getUntranslatedReturnsEmptyOverlay() {
        when(translationRepository.findByAudienceIdAndLocale(AUD, "es")).thenReturn(Optional.empty());

        AudienceTranslation t = new GetAudienceTranslationUseCase(
                audienceRepository, translationRepository, membershipCheck)
                .execute(OP, AUD, "es", USER);

        assertThat(t.name()).isNull();
        assertThat(t.locale().value()).isEqualTo("es");
        verify(membershipCheck).ensureMember(USER, OP);
    }

    @Test
    void listRequiresMemberAndScopesToAudience() {
        new ListAudienceTranslationsUseCase(audienceRepository, translationRepository, membershipCheck)
                .execute(OP, AUD, USER);

        verify(membershipCheck).ensureMember(USER, OP);
        verify(translationRepository).findAllByAudienceId(AUD);
    }

    @Test
    void deleteRemovesExistingOverlayAndAudits() {
        when(translationRepository.findByAudienceIdAndLocale(AUD, "es"))
                .thenReturn(Optional.of(overlay("Adultos")));

        delete().execute(OP, AUD, "es", USER);

        verify(membershipCheck).ensureAdmin(USER, OP);
        verify(translationRepository).deleteByAudienceIdAndLocale(AUD, "es");
        verify(auditTrailPort).append(any());
    }

    @Test
    void deleteOfAbsentOverlayIsIdempotentAndRecordsNothing() {
        delete().execute(OP, AUD, "es", USER);

        verify(translationRepository, never()).deleteByAudienceIdAndLocale(any(), any());
        verify(auditTrailPort, never()).append(any());
    }
}
