package com.example.inventory.web;

import com.example.inventory.adapter.in.web.InventoryController;
import com.example.inventory.adapter.in.web.dto.ReserveTicketRequest;
import com.example.inventory.application.port.in.ConfirmReservationUseCase;
import com.example.inventory.application.port.in.QueryInventoryUseCase;
import com.example.inventory.application.port.in.ReleaseReservationUseCase;
import com.example.inventory.application.port.in.ReserveTicketUseCase;
import com.example.inventory.domain.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.inventory.bootstrap.config.JwtAuthConverter;
import com.example.inventory.bootstrap.config.SecurityConfig;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@WebMvcTest(InventoryController.class)
@Import({SecurityConfig.class, JwtAuthConverter.class})
class InventoryControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private ReserveTicketUseCase reserveTicketUseCase;

    @MockBean
    private ConfirmReservationUseCase confirmReservationUseCase;

    @MockBean
    private ReleaseReservationUseCase releaseReservationUseCase;

    @MockBean
    private QueryInventoryUseCase queryInventoryUseCase;

    @Test
    @WithMockUser
    void shouldReserveTicket_whenAuthenticated() throws Exception {
        UUID showId = UUID.randomUUID();
        UUID ticketTypeId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Reservation reservation = Reservation.create(
                new TicketTypeId(ticketTypeId),
                new ShowId(showId),
                new UserId(userId),
                new RequestId(requestId),
                2,
                Instant.now(),
                15
        );

        when(reserveTicketUseCase.reserveTicket(any())).thenReturn(reservation);

        ReserveTicketRequest request = new ReserveTicketRequest(showId, ticketTypeId, 2, requestId);

        mockMvc.perform(post("/api/v1/inventories/reserve")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("HELD"))
                .andExpect(jsonPath("$.quantity").value(2));
    }

    @Test
    void shouldAllowPublicRead_onGetInventory() throws Exception {
        UUID showId = UUID.randomUUID();
        UUID ticketTypeId = UUID.randomUUID();
        Inventory inventory = Inventory.create(new ShowId(showId), new TicketTypeId(ticketTypeId), 100);

        when(queryInventoryUseCase.getInventory(any(), any())).thenReturn(inventory);

        mockMvc.perform(get("/api/v1/inventories/shows/" + showId + "/ticket-types/" + ticketTypeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuantity").value(100))
                .andExpect(jsonPath("$.availableQuantity").value(100));
    }

    @Test
    void shouldAllowPublicRead_onGetInventoriesByShow() throws Exception {
        UUID showId = UUID.randomUUID();
        UUID ticketTypeId = UUID.randomUUID();
        Inventory inventory = Inventory.create(new ShowId(showId), new TicketTypeId(ticketTypeId), 100);

        when(queryInventoryUseCase.getInventoriesByShow(any())).thenReturn(List.of(inventory));

        mockMvc.perform(get("/api/v1/inventories/shows/" + showId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].totalQuantity").value(100));
    }
}
