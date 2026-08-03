package com.vointika.storefront.infrastructure.security;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class HmacUnlockTokenTest {

    private static final UUID OPERATOR = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID ANOTHER_OPERATOR = UUID.fromString("00000000-0000-0000-0000-0000000000b2");

    private final HmacUnlockToken unlockToken = new HmacUnlockToken();

    @Test
    void aTokenVerifiesAgainstThePasswordItWasMintedFrom() {
        String token = unlockToken.compute("hunter2", OPERATOR);

        assertThat(unlockToken.matches(token, "hunter2", OPERATOR)).isTrue();
    }

    /**
     * The property the whole design was chosen for: the password <em>is</em> the
     * HMAC key, so rotating it invalidates every outstanding cookie with no
     * session store to sweep and no revocation list to keep.
     */
    @Test
    void changingThePasswordInvalidatesAnExistingToken() {
        String token = unlockToken.compute("hunter2", OPERATOR);

        assertThat(unlockToken.matches(token, "hunter3", OPERATOR)).isFalse();
    }

    /** Keying on the operator id means one tenant's cookie never unlocks another. */
    @Test
    void aTokenFromAnotherOperatorDoesNotVerify() {
        String token = unlockToken.compute("hunter2", ANOTHER_OPERATOR);

        assertThat(unlockToken.matches(token, "hunter2", OPERATOR)).isFalse();
    }

    @Test
    void garbageDoesNotVerify() {
        assertThat(unlockToken.matches("not-a-token", "hunter2", OPERATOR)).isFalse();
        assertThat(unlockToken.matches("", "hunter2", OPERATOR)).isFalse();
        assertThat(unlockToken.matches(null, "hunter2", OPERATOR)).isFalse();
    }

    /**
     * A token one byte off must fail, and it is the constant-time comparison that
     * has to say so — swap {@code MessageDigest.isEqual} for {@code equals} and
     * the answer stays right while the timing stops being.
     */
    @Test
    void aTokenOneCharacterOffDoesNotVerify() {
        String token = unlockToken.compute("hunter2", OPERATOR);
        char last = token.charAt(token.length() - 1);
        String tampered = token.substring(0, token.length() - 1) + (last == 'a' ? 'b' : 'a');

        assertThat(unlockToken.matches(tampered, "hunter2", OPERATOR)).isFalse();
    }

    /**
     * The gate enabled with no password set is reachable through the admin API.
     * Nothing may verify against it — least of all a visitor presenting nothing.
     */
    @Test
    void noPasswordMeansNoTokenVerifies() {
        assertThat(unlockToken.matches(null, null, OPERATOR)).isFalse();
        assertThat(unlockToken.matches("", null, OPERATOR)).isFalse();
        assertThat(unlockToken.matches("anything", null, OPERATOR)).isFalse();
        assertThat(unlockToken.matches("anything", "", OPERATOR)).isFalse();
    }

    @Test
    void theTokenIsHexAndCarriesNothingOfThePassword() {
        String token = unlockToken.compute("hunter2", OPERATOR);

        assertThat(token).hasSize(64).matches("[0-9a-f]+").doesNotContain("hunter2");
    }
}
