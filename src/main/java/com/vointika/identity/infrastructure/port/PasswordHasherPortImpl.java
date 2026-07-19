package com.vointika.identity.infrastructure.port;

import com.vointika.identity.application.port.PasswordHasherPort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordHasherPortImpl implements PasswordHasherPort {

    private final BCryptPasswordEncoder encoder;

    public PasswordHasherPortImpl() {
        this.encoder = new BCryptPasswordEncoder();
    }

    @Override
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String hashedPassword) {
        return encoder.matches(rawPassword, hashedPassword);
    }
}