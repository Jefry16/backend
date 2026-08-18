package com.vointika.shared.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The at-rest form of an opaque token, pinned against published SHA-256 vectors.
 *
 * <p><b>Neither adapter that computed this had a test.</b> `Sha256TokenHasherPortImpl`
 * and `InvitationTokenPortImpl` are mocked everywhere they are used, so the digest ran
 * in production and in nothing else — the same shape as `AudiencePricingResolver` in
 * #184: merging two copies is what made the absence visible.
 *
 * <p>It matters more here than most arithmetic. Every stored verification,
 * password-reset, refresh and invitation token is looked up <em>by this value</em>, so
 * a change to the algorithm, the encoding or the character set does not fail loudly —
 * it silently invalidates every token already issued, and every outstanding email link
 * stops working. The known-answer vectors below are what make that a build failure
 * instead of a support ticket.
 */
class TokenDigestTest {

    /** RFC 6234 / NIST test vector for the empty string. */
    @Test
    void hashesTheEmptyStringToTheKnownVector() {
        assertThat(TokenDigest.hexOf(""))
                .isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }

    /** RFC 6234 test vector for "abc" — pins the algorithm and the hex encoding. */
    @Test
    void hashesAbcToTheKnownVector() {
        assertThat(TokenDigest.hexOf("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    /**
     * UTF-8, not the platform default. A token is base64url in practice so this never
     * bites today, but the encoding is part of the stored form, and the two candidate
     * charsets genuinely disagree: the ISO-8859-1 reading of the same string is one
     * byte (0xE9) and digests to de2e33..., not 4a9955....
     */
    @Test
    void hashesNonAsciiAsUtf8() {
        assertThat(TokenDigest.hexOf("\u00e9"))
                .isEqualTo("4a99557e4033c3539de2eb65472017cad5f9557f7a0625a09f1c3f6e2ba69c4c")
                .isNotEqualTo("de2e331d891ae267a7009cb45b4e8830f170e0c937288ea2731a1941c7a53b0d");
    }

    @Test
    void isDeterministicAndLowerCaseHexOfFixedWidth() {
        String once = TokenDigest.hexOf("a-token-value");
        assertThat(TokenDigest.hexOf("a-token-value")).isEqualTo(once);
        assertThat(once).hasSize(64).matches("[0-9a-f]{64}");
    }

    @Test
    void differentTokensDigestDifferently() {
        assertThat(TokenDigest.hexOf("token-a")).isNotEqualTo(TokenDigest.hexOf("token-b"));
    }
}
