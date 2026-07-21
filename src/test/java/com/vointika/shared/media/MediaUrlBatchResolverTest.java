package com.vointika.shared.media;

import com.vointika.shared.port.MediaKeyBatchQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MediaUrlBatchResolverTest {

    private MediaKeyBatchQuery mediaKeyBatchQuery;
    private MediaUrlBatchResolver resolver;

    private final UUID operatorId = UUID.randomUUID();
    private final UUID mediaId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mediaKeyBatchQuery = mock(MediaKeyBatchQuery.class);
        resolver = new MediaUrlBatchResolver(mediaKeyBatchQuery, new MediaUrlResolver("https://cdn.example.com"));
    }

    @Test
    void resolvesKeyToAbsoluteUrl() {
        when(mediaKeyBatchQuery.findKeysByIds(operatorId, Set.of(mediaId)))
                .thenReturn(Map.of(mediaId, "tour-operators/x/logo.png"));

        assertEquals("https://cdn.example.com/tour-operators/x/logo.png",
                resolver.resolveOne(operatorId, mediaId));
    }

    @Test
    void nullIdShortCircuitsWithoutQuerying() {
        assertNull(resolver.resolveOne(operatorId, null));
        verify(mediaKeyBatchQuery, never()).findKeysByIds(any(), any());
    }

    @Test
    void unownedOrDeletedMediaResolvesToNull() {
        when(mediaKeyBatchQuery.findKeysByIds(operatorId, Set.of(mediaId))).thenReturn(Map.of());
        assertNull(resolver.resolveOne(operatorId, mediaId));
    }
}
