package com.vointika.identity.application.port;

import java.io.InputStream;

/**
 * Object storage for user avatars. Identity-owned mirror of the media
 * context's storage port — avatars are user-scoped (a user spans operators),
 * so they cannot live in the tenant-scoped media library.
 */
public interface AvatarStoragePort {
    void putObject(String key, String contentType, long size, InputStream body);
    /** Best effort: never throws. A failure is logged by the adapter and leaves
     *  an orphaned object, because the row it belonged to is already gone. */
    void deleteObject(String key);
}
