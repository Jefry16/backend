package com.vointika.identity.application.port;

import java.io.InputStream;

/**
 * Object storage for user avatars. Identity-owned mirror of the media
 * context's storage port — avatars are user-scoped (a user spans operators),
 * so they cannot live in the tenant-scoped media library.
 */
public interface AvatarStoragePort {
    void putObject(String key, String contentType, long size, InputStream body);
    void deleteObject(String key);
}
