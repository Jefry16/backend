package com.vointika.shared.media;

import com.vointika.shared.port.MediaAssetBatchQuery;
import com.vointika.shared.port.MediaAssetBatchQuery.MediaAsset;
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

    private MediaAssetBatchQuery mediaAssetBatchQuery;
    private MediaUrlBatchResolver resolver;

    private final UUID operatorId = UUID.randomUUID();
    private final UUID mediaId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mediaAssetBatchQuery = mock(MediaAssetBatchQuery.class);
        resolver = new MediaUrlBatchResolver(mediaAssetBatchQuery, new MediaUrlResolver("https://cdn.example.com"));
    }

    @Test
    void resolvesKeyToAbsoluteUrl() {
        when(mediaAssetBatchQuery.findAssetsByIds(operatorId, Set.of(mediaId)))
                .thenReturn(Map.of(mediaId, asset("tour-operators/x/logo.png")));

        assertEquals("https://cdn.example.com/tour-operators/x/logo.png",
                resolver.resolveOne(operatorId, mediaId));
    }

    @Test
    void nullIdShortCircuitsWithoutQuerying() {
        assertNull(resolver.resolveOne(operatorId, null));
        verify(mediaAssetBatchQuery, never()).findAssetsByIds(any(), any());
    }

    @Test
    void unownedOrDeletedMediaResolvesToNull() {
        when(mediaAssetBatchQuery.findAssetsByIds(operatorId, Set.of(mediaId))).thenReturn(Map.of());
        assertNull(resolver.resolveOne(operatorId, mediaId));
    }

    /** This resolver wants the key alone; alt and the dimensions are another caller's. */
    private static MediaAsset asset(String storageKey) {
        return new MediaAsset(storageKey, null, null, null);
    }
}
