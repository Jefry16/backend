package com.vointika.identity.infrastructure.port;

import com.vointika.shared.service.TokenDigest;
import com.vointika.identity.application.port.TokenHasherPort;
import org.springframework.stereotype.Component;


@Component
public class Sha256TokenHasherPortImpl implements TokenHasherPort {

    @Override
    public String hash(String rawToken) {
        return TokenDigest.hexOf(rawToken);
    }
}
