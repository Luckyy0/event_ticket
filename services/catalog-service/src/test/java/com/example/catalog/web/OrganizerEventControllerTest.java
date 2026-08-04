package com.example.catalog.web;

import com.example.catalog.adapter.in.web.GlobalExceptionHandler;
import com.example.catalog.adapter.in.web.OrganizerEventController;
import com.example.catalog.application.port.in.CreateEventUseCase;
import com.example.catalog.application.port.in.ManageEventUseCase;
import com.example.catalog.application.port.in.ManageShowUseCase;
import com.example.catalog.bootstrap.config.JwtAuthConverter;
import com.example.catalog.bootstrap.config.SecurityConfig;
import com.example.catalog.domain.model.Event;
import com.example.catalog.domain.model.Venue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrganizerEventController.class)
@Import({SecurityConfig.class, JwtAuthConverter.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class OrganizerEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreateEventUseCase createEventUseCase;

    @MockBean
    private ManageEventUseCase manageEventUseCase;

    @MockBean
    private ManageShowUseCase manageShowUseCase;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void shouldReturn401_whenUnauthorizedUserTriesToCreateEvent() throws Exception {
        String requestJson = """
                {
                    "name": "Unauthorized Concert",
                    "venue": {
                        "name": "Arena",
                        "city": "Hanoi"
                    }
                }
                """;

        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403_whenCustomerTriesToCreateEvent() throws Exception {
        String requestJson = """
                {
                    "name": "Customer Event",
                    "venue": {
                        "name": "Arena",
                        "city": "Hanoi"
                    }
                }
                """;

        mockMvc.perform(post("/api/v1/events")
                        .with(jwt().authorities(() -> "ROLE_CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldCreateEvent_whenUserIsOrganizer() throws Exception {
        UUID organizerId = UUID.randomUUID();
        Venue venue = new Venue("Arena Grand", "123 Street", "Hanoi", 5000);
        Event createdEvent = Event.create("Grand Symphony", "Symphony desc", "https://img.com/sym.jpg", organizerId, venue);

        when(createEventUseCase.createEvent(any())).thenReturn(createdEvent);

        String requestJson = """
                {
                    "name": "Grand Symphony",
                    "description": "Symphony desc",
                    "imageUrl": "https://img.com/sym.jpg",
                    "venue": {
                        "name": "Arena Grand",
                        "address": "123 Street",
                        "city": "Hanoi",
                        "capacity": 5000
                    }
                }
                """;

        mockMvc.perform(post("/api/v1/events")
                        .with(jwt().jwt(jwt -> jwt.subject(organizerId.toString()))
                                .authorities(() -> "ROLE_EVENT_ORGANIZER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Grand Symphony"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void shouldPublishEvent_whenUserIsOrganizer() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID organizerId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/events/{eventId}/publish", eventId)
                        .with(jwt().jwt(jwt -> jwt.subject(organizerId.toString()))
                                .authorities(() -> "ROLE_EVENT_ORGANIZER")))
                .andExpect(status().isNoContent());
    }
}
