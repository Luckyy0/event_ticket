package com.example.catalog.application.command;

import com.example.catalog.domain.model.Venue;

import java.util.UUID;

public record CreateEventCommand(
        String name,
        String description,
        String imageUrl,
        UUID organizerId,
        Venue venue
) {}
