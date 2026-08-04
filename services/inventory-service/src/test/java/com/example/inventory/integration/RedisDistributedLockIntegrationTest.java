package com.example.inventory.integration;

import com.example.inventory.application.port.out.DistributedLockPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RedisDistributedLockIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private DistributedLockPort distributedLockPort;

    @Test
    void shouldAcquireAndReleaseLock() {
        String lockKey = "lock:test:" + UUID.randomUUID();
        String lockValue = UUID.randomUUID().toString();

        boolean acquired = distributedLockPort.acquireLock(lockKey, lockValue, Duration.ofSeconds(5), Duration.ofSeconds(1));
        assertThat(acquired).isTrue();

        // Secondary attempt with different value should fail immediately if waitTime is 0
        boolean acquiredSecond = distributedLockPort.acquireLock(lockKey, UUID.randomUUID().toString(), Duration.ofSeconds(5), Duration.ofMillis(100));
        assertThat(acquiredSecond).isFalse();

        // Release lock
        boolean released = distributedLockPort.releaseLock(lockKey, lockValue);
        assertThat(released).isTrue();

        // Now third attempt should succeed
        boolean acquiredThird = distributedLockPort.acquireLock(lockKey, UUID.randomUUID().toString(), Duration.ofSeconds(5), Duration.ofMillis(500));
        assertThat(acquiredThird).isTrue();
    }

    @Test
    void shouldNotReleaseLock_whenValueDoesNotMatch() {
        String lockKey = "lock:test:" + UUID.randomUUID();
        String ownerValue = UUID.randomUUID().toString();
        String intruderValue = UUID.randomUUID().toString();

        boolean acquired = distributedLockPort.acquireLock(lockKey, ownerValue, Duration.ofSeconds(5), Duration.ofSeconds(1));
        assertThat(acquired).isTrue();

        // Intruder attempts to release owner's lock
        boolean intruderReleased = distributedLockPort.releaseLock(lockKey, intruderValue);
        assertThat(intruderReleased).isFalse();

        // Owner can still release
        boolean ownerReleased = distributedLockPort.releaseLock(lockKey, ownerValue);
        assertThat(ownerReleased).isTrue();
    }
}
