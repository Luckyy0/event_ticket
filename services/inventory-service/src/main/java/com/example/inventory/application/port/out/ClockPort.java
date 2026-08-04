package com.example.inventory.application.port.out;

import java.time.Instant;

public interface ClockPort {
    Instant now();
}
