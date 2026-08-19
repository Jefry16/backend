package com.vointika.media.application.usecase;

import com.vointika.media.application.dto.input.DescribeMediaInput;
import com.vointika.media.domain.entity.Media;
import com.vointika.media.domain.repository.MediaRepository;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Alt text on a stored asset — the one media use case that had no test at all.
 *
 * <p>It surfaced as the third call site of {@code requireByIdAndTourOperatorId}: breaking
 * that default failed the delete and get tests and left this one silent. Without
 * {@link #describingSomethingTheOperatorDoesNotOwnIs404} the mutation returns null here,
 * {@code media.getAlt()} NPEs, and {@code PATCH .../media/{id}} on a foreign id answers
 * <b>500 where the tenant-isolation 404 belongs</b> — with the suite green. Same shape as
 * `pickup`'s untested read in #188.
 */
class DescribeMediaUseCaseTest {

    private MediaRepository mediaRepository;
    private TourOperatorMembershipCheck membershipCheck;
    private AuditTrailPort auditTrailPort;
    private DescribeMediaUseCase useCase;

    private final UUID operatorId = UUID.randomUUID();
    private final UUID mediaId = UUID.randomUUID();
    private final UUID callerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mediaRepository = mock(MediaRepository.class);
        // requireByIdAndTourOperatorId is a default method: Mockito would stub it to
        // null and the 404 assertion below would pass without running the branch.
        doCallRealMethod().when(mediaRepository).requireByIdAndTourOperatorId(any(), any());
        membershipCheck = mock(TourOperatorMembershipCheck.class);
        auditTrailPort = mock(AuditTrailPort.class);
        useCase = new DescribeMediaUseCase(mediaRepository, membershipCheck,
                executingRunner(), auditTrailPort);
    }

    /** The tx lambda has to actually run, or nothing below observes a save. */
    private static TransactionRunner executingRunner() {
        TransactionRunner runner = mock(TransactionRunner.class);
        doAnswer(i -> {
            ((Runnable) i.getArgument(0)).run();
            return null;
        }).when(runner).run(any());
        return runner;
    }

    private Media media(String alt) {
        Media media = new Media(mediaId, operatorId, "tour-operators/x/y.png",
                "image/png", 100, "y.png", callerId, "Uma Uploader",
                Instant.parse("2026-07-21T00:00:00Z"), null, null, null);
        if (alt != null) {
            media.describe(new com.vointika.media.domain.valueobject.MediaAlt(alt));
        }
        return media;
    }

    @Test
    void describingSomethingTheOperatorDoesNotOwnIs404() {
        when(mediaRepository.findByIdAndTourOperatorId(mediaId, operatorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(
                operatorId, mediaId, new DescribeMediaInput("A harbour at dusk"), callerId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(MediaRepository.NOT_FOUND);

        verify(mediaRepository, never()).save(any());
        verify(auditTrailPort, never()).append(any());
    }

    @Test
    void writingAltTextSavesAndAudits() {
        when(mediaRepository.findByIdAndTourOperatorId(mediaId, operatorId)).thenReturn(Optional.of(media(null)));

        useCase.execute(operatorId, mediaId, new DescribeMediaInput("A harbour at dusk"), callerId);

        verify(membershipCheck).ensureAdmin(callerId, operatorId);
        ArgumentCaptor<Media> saved = ArgumentCaptor.forClass(Media.class);
        verify(mediaRepository).save(saved.capture());
        assertThat(saved.getValue().getAlt().value()).isEqualTo("A harbour at dusk");

        ArgumentCaptor<NewAuditEntry> entry = ArgumentCaptor.forClass(NewAuditEntry.class);
        verify(auditTrailPort).append(entry.capture());
        assertThat(entry.getValue().action()).isEqualTo("media.described");
    }

    /** Blank clears the alt rather than storing an empty string. */
    @Test
    void blankAltClearsIt() {
        when(mediaRepository.findByIdAndTourOperatorId(mediaId, operatorId))
                .thenReturn(Optional.of(media("Old text")));

        useCase.execute(operatorId, mediaId, new DescribeMediaInput("  "), callerId);

        ArgumentCaptor<Media> saved = ArgumentCaptor.forClass(Media.class);
        verify(mediaRepository).save(saved.capture());
        assertThat(saved.getValue().getAlt()).isNull();
    }

    /** Re-sending the same alt writes nothing and records nothing. */
    @Test
    void anUnchangedAltIsANoOp() {
        when(mediaRepository.findByIdAndTourOperatorId(mediaId, operatorId))
                .thenReturn(Optional.of(media("A harbour at dusk")));

        useCase.execute(operatorId, mediaId, new DescribeMediaInput("A harbour at dusk"), callerId);

        verify(mediaRepository, never()).save(any());
        verify(auditTrailPort, never()).append(any());
    }
}
