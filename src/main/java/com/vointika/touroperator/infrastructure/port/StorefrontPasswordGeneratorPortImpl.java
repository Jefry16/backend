package com.vointika.touroperator.infrastructure.port;

import com.vointika.touroperator.application.port.StorefrontPasswordGeneratorPort;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Twelve characters from an alphabet with the look-alikes removed.
 *
 * <p><b>No {@code 0}/{@code O}, {@code 1}/{@code l}/{@code I}</b>, because this
 * password gets read off a screen and typed by hand, or dictated over the phone
 * to somebody the operator wants to show the store to. That is the whole use
 * case, and an ambiguous glyph turns it into a support message.
 *
 * <p>Twelve characters of a 54-symbol alphabet is about 69 bits, which is far
 * more than the 20-attempts-an-hour rate limit in front of it needs — the limit
 * is what makes guessing hopeless; the length is what makes a leaked-and-rotated
 * password a non-event.
 */
@Component
public class StorefrontPasswordGeneratorPortImpl implements StorefrontPasswordGeneratorPort {

    private static final String ALPHABET = "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int LENGTH = 12;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generate() {
        StringBuilder password = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            password.append(ALPHABET.charAt(secureRandom.nextInt(ALPHABET.length())));
        }
        return password.toString();
    }
}
