package com.vointika.touroperator.application.usecase;

import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.MediaAssetBatchQuery;
import com.vointika.shared.port.MediaAssetBatchQuery.MediaAsset;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.LocaleCode;
import com.vointika.shared.valueobject.Handle;
import com.vointika.touroperator.domain.entity.TourOperator;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;
import com.vointika.touroperator.domain.valueobject.TourOperatorAddress;
import com.vointika.touroperator.domain.valueobject.TourOperatorName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UpdateOperatorSeoUseCaseTest {

    private static final UUID OP = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");
    private static final UUID USER = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID MEDIA = UUID.fromString("aaaaaaaa-0000-4000-8000-000000000001");

    private TourOperatorRepository operatorRepository;
    private MediaAssetBatchQuery mediaAssetBatchQuery;
    private TourOperatorMembershipCheck membershipCheck;
    private TransactionRunner transactionRunner;
    private AuditTrailPort auditTrailPort;
    private TourOperator operator;

    @BeforeEach
    void setUp() {
        operatorRepository = mock(TourOperatorRepository.class);
        mediaAssetBatchQuery = mock(MediaAssetBatchQuery.class);
        membershipCheck = mock(TourOperatorMembershipCheck.class);
        transactionRunner = mock(TransactionRunner.class);
        auditTrailPort = mock(AuditTrailPort.class);
        doAnswer(i -> {
            ((Runnable) i.getArgument(0)).run();
            return null;
        }).when(transactionRunner).run(any());
        operator = new TourOperator(OP, new TourOperatorName("Acme"), new Handle("acme"),
                UUID.randomUUID(), UUID.randomUUID(), new TourOperatorAddress("Somewhere 1"),
                USER, Instant.now(), Instant.now(),
                LocaleCode.of("en"), Set.of(LocaleCode.of("en")));
        when(operatorRepository.findById(OP)).thenReturn(Optional.of(operator));
    }

    private UpdateOperatorSeoUseCase useCase() {
        return new UpdateOperatorSeoUseCase(operatorRepository, mediaAssetBatchQuery,
                membershipCheck, transactionRunner, auditTrailPort);
    }

    @Test
    void storesTheDefaultsAndAudits() {
        when(mediaAssetBatchQuery.findAssetsByIds(OP, Set.of(MEDIA))).thenReturn(Map.of(MEDIA, new MediaAsset("k", null, null, null)));

        useCase().execute(OP, "Acme Tours — diving", "Small-group diving.", MEDIA, USER);

        verify(membershipCheck).ensureAdmin(USER, OP);
        verify(operatorRepository).save(operator);
        assertThat(operator.getSeoTitle().value()).isEqualTo("Acme Tours — diving");
        assertThat(operator.getOgImageMediaId()).isEqualTo(MEDIA);
        ArgumentCaptor<NewAuditEntry> audit = ArgumentCaptor.forClass(NewAuditEntry.class);
        verify(auditTrailPort).append(audit.capture());
        assertThat(audit.getValue().action()).isEqualTo("tour_operator.seo_updated");
    }

    @Test
    void aNullFieldClearsTheOverrideRatherThanKeepingIt() {
        // Whole-value replace: this is how an operator removes a default, and a
        // partial patch could not express it without a sentinel.
        operator.updateSeo(new com.vointika.touroperator.domain.valueobject.OperatorSeoTitle("Old"), null, null);

        useCase().execute(OP, null, null, null, USER);

        assertThat(operator.getSeoTitle()).isNull();
    }

    @Test
    void rejectsAnOgImageOutsideThisOperatorsLibrary() {
        // A bare media id with no FK is only as trustworthy as the check admitting it.
        when(mediaAssetBatchQuery.findAssetsByIds(OP, Set.of(MEDIA))).thenReturn(Map.of());

        assertThatThrownBy(() -> useCase().execute(OP, "t", null, MEDIA, USER))
                .isInstanceOf(InvalidFieldException.class);

        verify(operatorRepository, never()).save(any());
        verify(auditTrailPort, never()).append(any());
    }

    @Test
    void aNoOpUpdateRecordsNothing() {
        useCase().execute(OP, null, null, null, USER);

        verify(operatorRepository, never()).save(any());
        verify(auditTrailPort, never()).append(any());
    }

    @Test
    void requiresAdmin() {
        doThrow(new ForbiddenException("admin")).when(membershipCheck).ensureAdmin(USER, OP);

        assertThatThrownBy(() -> useCase().execute(OP, "t", null, null, USER))
                .isInstanceOf(ForbiddenException.class);
        verify(operatorRepository, never()).save(any());
    }
}
