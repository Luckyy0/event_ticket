package com.example.catalog.web;

import com.example.catalog.adapter.in.web.EventController;
import com.example.catalog.adapter.in.web.GlobalExceptionHandler;
import com.example.catalog.application.port.in.QueryEventUseCase;
import com.example.catalog.bootstrap.config.JwtAuthConverter;
import com.example.catalog.bootstrap.config.SecurityConfig;
import com.example.catalog.domain.exception.EventNotFoundException;
import com.example.catalog.domain.model.Event;
import com.example.catalog.domain.model.Venue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
@Import({SecurityConfig.class, JwtAuthConverter.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QueryEventUseCase queryEventUseCase;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void shouldReturnPublishedEvents_withoutAuthentication() throws Exception {
        UUID organizerId = UUID.randomUUID();
        Venue venue = new Venue("Saigon Center", "Street 1", "Ho Chi Minh", 5000);
        Event event = Event.create("Pop Music Fest", "Desc", "https://img.com/fest.jpg", organizerId, venue);
        event.publish();

        when(queryEventUseCase.listPublishedEvents(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(event), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Pop Music Fest"))
                .andExpect(jsonPath("$.content[0].venue.city").value("Ho Chi Minh"));
    }

    @Test
    void shouldReturnEventDetail_whenEventExists() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID organizerId = UUID.randomUUID();
        Venue venue = new Venue("Saigon Center", "Street 1", "Ho Chi Minh", 5000);
        Event event = new Event(eventId, "Pop Music Fest", "Desc", null, organizerId, venue, null, null, null, null);

        when(queryEventUseCase.getEventById(eventId)).thenReturn(event);

        mockMvc.perform(get("/api/v1/events/{eventId}", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(eventId.toString()))
                .andExpect(jsonPath("$.name").value("Pop Music Fest"));
    }

    @Test
    void shouldReturn404_whenEventDoesNotExist() throws Exception {
        UUID eventId = UUID.randomUUID();
        when(queryEventUseCase.getEventById(eventId)).thenThrow(new EventNotFoundException(eventId));

        mockMvc.perform(get("/api/v1/events/{eventId}", eventId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }
}
