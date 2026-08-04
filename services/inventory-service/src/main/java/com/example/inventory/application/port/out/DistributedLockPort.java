package com.example.inventory.application.port.out;

import java.time.Duration;

public interface DistributedLockPort {
    boolean acquireLock(String lockKey, String lockValue, Duration leaseTime, Duration waitTime);
    boolean releaseLock(String lockKey, String lockValue);
}
