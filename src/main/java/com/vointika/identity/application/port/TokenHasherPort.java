package com.vointika.identity.application.port;

public interface TokenHasherPort {
    String hash(String rawToken);
}
