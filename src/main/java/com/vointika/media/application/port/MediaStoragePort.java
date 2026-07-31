package com.vointika.media.application.port;

import java.io.InputStream;

/**
 * Object storage for the media library — the media context's own storage seam
 * (identity's {@code AvatarStoragePort} is the user-scoped mirror of this).
 * Keys are namespaced under {@code tour-operators/…}.
 */
public interface MediaStoragePort {
    void putObject(String key, String contentType, long size, InputStream body);
    /** Best effort: never throws. A failure is logged by the adapter and leaves
     *  an orphaned object, because the row it belonged to is already gone. */
    void deleteObject(String key);
}
