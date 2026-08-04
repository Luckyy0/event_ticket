package com.example.inventory.adapter.out.lock;

import com.example.inventory.application.port.out.DistributedLockPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;

@Component
public class RedisDistributedLockAdapter implements DistributedLockPort {

    private static final Logger log = LoggerFactory.getLogger(RedisDistributedLockAdapter.class);

    private static final String UNLOCK_LUA_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "    return redis.call('del', KEYS[1]) " +
            "else " +
            "    return 0 " +
            "end";

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> unlockScript;

    public RedisDistributedLockAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.unlockScript = new DefaultRedisScript<>(UNLOCK_LUA_SCRIPT, Long.class);
    }

    @Override
    public boolean acquireLock(String lockKey, String lockValue, Duration leaseTime, Duration waitTime) {
        long startTime = System.currentTimeMillis();
        long waitTimeMs = waitTime.toMillis();

        do {
            try {
                Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, leaseTime);
                if (Boolean.TRUE.equals(acquired)) {
                    return true;
                }
            } catch (Exception ex) {
                log.warn("Error attempting to acquire Redis lock for key {}: {}", lockKey, ex.getMessage());
            }

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        } while (System.currentTimeMillis() - startTime < waitTimeMs);

        return false;
    }

    @Override
    public boolean releaseLock(String lockKey, String lockValue) {
        try {
            Long result = redisTemplate.execute(
                    unlockScript,
                    Collections.singletonList(lockKey),
                    lockValue
            );
            return result != null && result > 0;
        } catch (Exception ex) {
            log.error("Error executing Redis unlock script for key {}: {}", lockKey, ex.getMessage());
            return false;
        }
    }
}
