package com.vointika.experience.application.service;

import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.port.MediaKeyBatchQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MediaReferenceValidatorTest {

    private MediaKeyBatchQuery mediaKeyBatchQuery;
    private MediaReferenceValidator validator;
    private final UUID operatorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mediaKeyBatchQuery = mock(MediaKeyBatchQuery.class);
        validator = new MediaReferenceValidator(mediaKeyBatchQuery);
    }

    @Test
    void passesWhenAllIdsOwned() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        when(mediaKeyBatchQuery.findKeysByIds(eq(operatorId), any()))
                .thenReturn(Map.of(a, "k1", b, "k2"));
        validator.validate(operatorId, List.of(a), b); // union {a,b}, both owned
    }

    @Test
    void rejectsWhenAnyIdIsForeign() {
        UUID a = UUID.randomUUID();
        UUID foreign = UUID.randomUUID();
        when(mediaKeyBatchQuery.findKeysByIds(eq(operatorId), any()))
                .thenReturn(Map.of(a, "k1")); // foreign absent
        assertThrows(InvalidFieldException.class,
                () -> validator.validate(operatorId, List.of(a, foreign), null));
    }

    @Test
    void noIdsShortCircuits() {
        validator.validate(operatorId, List.of(), null);
        verify(mediaKeyBatchQuery, never()).findKeysByIds(any(), any());
    }
}
