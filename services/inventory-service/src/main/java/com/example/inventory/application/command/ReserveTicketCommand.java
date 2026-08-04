package com.example.inventory.application.command;

import com.example.inventory.domain.model.RequestId;
import com.example.inventory.domain.model.ShowId;
import com.example.inventory.domain.model.TicketTypeId;
import com.example.inventory.domain.model.UserId;

public record ReserveTicketCommand(
        TicketTypeId ticketTypeId,
        ShowId showId,
        UserId userId,
        int quantity,
        RequestId requestId
) {
    public ReserveTicketCommand {
        if (ticketTypeId == null) throw new IllegalArgumentException("TicketTypeId cannot be null");
        if (showId == null) throw new IllegalArgumentException("ShowId cannot be null");
        if (userId == null) throw new IllegalArgumentException("UserId cannot be null");
        if (requestId == null) throw new IllegalArgumentException("RequestId cannot be null");
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be greater than zero");
    }
}
