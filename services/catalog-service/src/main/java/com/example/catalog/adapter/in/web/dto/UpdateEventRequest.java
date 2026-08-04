package com.example.catalog.adapter.in.web.dto;

import com.example.catalog.domain.model.Venue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateEventRequest(
        @NotBlank(message = "Event name is required")
        @Size(max = 200, message = "Event name cannot exceed 200 characters")
        String name,

        @Size(max = 5000, message = "Description cannot exceed 5000 characters")
        String description,

        String imageUrl,

        @NotNull(message = "Venue is required")
        CreateEventRequest.VenueDto venue
) {}
