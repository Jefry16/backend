package com.vointika.shared.infrastructure.redis;

import com.vointika.shared.port.RateLimiterPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis-backed fixed-window counter: INCR the key; the attempt that opens a
 * window (count == 1) sets the expiry. The INCR/EXPIRE pair is not atomic —
 * a crash between them could leave a counter without TTL — so EXPIRE is
 * re-asserted whenever the key reports no TTL. Single-key operations only
 * (no CROSSSLOT concern under the §2 topology).
 *
 * <p><b>Fail-open with a cool-down:</b> any Redis failure allows the request
 * and trips a short circuit so a dead Redis costs one failed call per
 * cool-down period, not one per request.
 */
@Component
public class RedisRateLimiter implements RateLimiterPort {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimiter.class);

    /** After a Redis failure, skip the store entirely for this long. */
    private static final Duration FAILURE_COOLDOWN = Duration.ofSeconds(10);

    private final StringRedisTemplate redis;
    private volatile long retryAtMillis;

    public RedisRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public boolean tryAcquire(String key, int limit, Duration window) {
        if (System.currentTimeMillis() < retryAtMillis) {
            return true; // circuit open — fail-open without dialing Redis
        }
        try {
            Long count = redis.opsForValue().increment(key);
            if (count == null) {
                return true;
            }
            if (count == 1L) {
                redis.expire(key, window);
            } else {
                Long ttl = redis.getExpire(key);
                if (ttl != null && ttl < 0) {
                    // Lost-expiry repair (crash between INCR and EXPIRE).
                    redis.expire(key, window);
                }
            }
            return count <= limit;
        } catch (RuntimeException e) {
            retryAtMillis = System.currentTimeMillis() + FAILURE_COOLDOWN.toMillis();
            log.warn("Rate limiter unavailable — failing open for {}s: {}",
                    FAILURE_COOLDOWN.toSeconds(), e.getMessage());
            return true;
        }
    }
}
