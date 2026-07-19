package com.vointika.shared.infrastructure.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Counter semantics against a mocked {@link StringRedisTemplate}: window
 * opening (count == 1 sets the expiry), lost-expiry repair, the limit
 * boundary, and the fail-open + cool-down circuit on a Redis outage.
 */
@ExtendWith(MockitoExtension.class)
class RedisRateLimiterTest {

    private static final Duration WINDOW = Duration.ofMinutes(15);

    @Mock private StringRedisTemplate redis;
    @Mock private ValueOperations<String, String> valueOps;

    private RedisRateLimiter limiter;

    @BeforeEach
    void setUp() {
        limiter = new RedisRateLimiter(redis);
    }

    @Test
    void allowsWhenCountIsWithinLimit() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment("rl:test")).thenReturn(2L);
        when(redis.getExpire("rl:test")).thenReturn(600L);

        assertTrue(limiter.tryAcquire("rl:test", 5, WINDOW));
    }

    @Test
    void deniesWhenCountExceedsLimit() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment("rl:test")).thenReturn(6L);
        when(redis.getExpire("rl:test")).thenReturn(600L);

        assertFalse(limiter.tryAcquire("rl:test", 5, WINDOW));
    }

    @Test
    void firstAttemptInWindowSetsExpiry() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment("rl:test")).thenReturn(1L);

        assertTrue(limiter.tryAcquire("rl:test", 5, WINDOW));

        verify(redis).expire("rl:test", WINDOW);
        verify(redis, never()).getExpire(any());
    }

    @Test
    void repairsExpiryWhenKeyHasNoTtl() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment("rl:test")).thenReturn(3L);
        when(redis.getExpire("rl:test")).thenReturn(-1L);

        assertTrue(limiter.tryAcquire("rl:test", 5, WINDOW));

        verify(redis).expire("rl:test", WINDOW);
    }

    @Test
    void failsOpenOnRedisErrorAndSkipsRedisDuringCooldown() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment("rl:test")).thenThrow(new RedisConnectionFailureException("down"));

        assertTrue(limiter.tryAcquire("rl:test", 5, WINDOW), "fail-open on Redis outage");

        verify(redis).opsForValue();
        verify(valueOps).increment("rl:test");

        // Circuit open: the next call must not dial Redis at all
        assertTrue(limiter.tryAcquire("rl:test", 5, WINDOW));
        verifyNoMoreInteractions(redis, valueOps);
    }
}
