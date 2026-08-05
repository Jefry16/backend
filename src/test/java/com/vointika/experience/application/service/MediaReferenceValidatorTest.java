package com.vointika.experience.application.service;

import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.port.MediaAssetBatchQuery;
import com.vointika.shared.port.MediaAssetBatchQuery.MediaAsset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MediaReferenceValidatorTest {

    private MediaAssetBatchQuery mediaAssetBatchQuery;
    private MediaReferenceValidator validator;
    private final UUID operatorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mediaAssetBatchQuery = mock(MediaAssetBatchQuery.class);
        validator = new MediaReferenceValidator(mediaAssetBatchQuery);
    }

    @Test
    void passesWhenAllIdsOwned() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        when(mediaAssetBatchQuery.findAssetsByIds(eq(operatorId), any()))
                .thenReturn(Map.of(a, asset("k1"), b, asset("k2")));
        validator.validate(operatorId, List.of(a), b); // union {a,b}, both owned
    }

    @Test
    void rejectsWhenAnyIdIsForeign() {
        UUID a = UUID.randomUUID();
        UUID foreign = UUID.randomUUID();
        when(mediaAssetBatchQuery.findAssetsByIds(eq(operatorId), any()))
                .thenReturn(Map.of(a, asset("k1"))); // foreign absent
        assertThrows(InvalidFieldException.class,
                () -> validator.validate(operatorId, List.of(a, foreign), null));
    }

    @Test
    void noIdsShortCircuits() {
        validator.validate(operatorId, List.of(), null);
        verify(mediaAssetBatchQuery, never()).findAssetsByIds(any(), any());
    }

    /** The validator only asks whether the id came back; the rest of the asset is noise here. */
    private static MediaAsset asset(String storageKey) {
        return new MediaAsset(storageKey, null, null, null);
    }
}
