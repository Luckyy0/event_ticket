package com.example.catalog.adapter.in.web.dto;

import java.time.Instant;

public record SaleWindowResponse(
        Instant opensAt,
        Instant closesAt
) {}
