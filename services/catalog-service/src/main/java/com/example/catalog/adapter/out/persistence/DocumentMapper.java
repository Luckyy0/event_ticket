package com.example.catalog.adapter.out.persistence;

import com.example.catalog.domain.model.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public final class DocumentMapper {

    private DocumentMapper() {}

    public static EventDocument toDocument(Event domain) {
        if (domain == null) return null;

        EventDocument.VenueDocument venueDoc = domain.getVenue() != null
                ? new EventDocument.VenueDocument(
                domain.getVenue().getName(),
                domain.getVenue().getAddress(),
                domain.getVenue().getCity(),
                domain.getVenue().getCapacity())
                : null;

        List<EventDocument.ShowDocument> showDocs = domain.getShows().stream()
                .map(DocumentMapper::toShowDocument)
                .collect(Collectors.toList());

        return new EventDocument(
                domain.getId().toString(),
                domain.getName(),
                domain.getDescription(),
                domain.getImageUrl(),
                domain.getOrganizerId().toString(),
                venueDoc,
                domain.getStatus().name(),
                showDocs,
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }

    public static EventDocument.ShowDocument toShowDocument(Show show) {
        if (show == null) return null;

        EventDocument.SaleWindowDocument saleWindowDoc = show.getSaleWindow() != null
                ? new EventDocument.SaleWindowDocument(
                show.getSaleWindow().getOpensAt(),
                show.getSaleWindow().getClosesAt())
                : null;

        List<EventDocument.TicketTypeDocument> ticketDocs = show.getTicketTypes().stream()
                .map(t -> new EventDocument.TicketTypeDocument(
                        t.getId().toString(),
                        t.getName(),
                        t.getDescription(),
                        t.getPrice(),
                        t.getCurrency(),
                        t.getTotalQuantity(),
                        t.getSortOrder()))
                .collect(Collectors.toList());

        return new EventDocument.ShowDocument(
                show.getId().toString(),
                show.getEventId().toString(),
                show.getStartTime(),
                show.getEndTime(),
                show.getStatus().name(),
                saleWindowDoc,
                ticketDocs
        );
    }

    public static Event toDomain(EventDocument doc) {
        if (doc == null) return null;

        Venue venue = doc.getVenue() != null
                ? new Venue(
                doc.getVenue().getName(),
                doc.getVenue().getAddress(),
                doc.getVenue().getCity(),
                doc.getVenue().getCapacity())
                : null;

        List<Show> shows = doc.getShows().stream()
                .map(DocumentMapper::toShowDomain)
                .collect(Collectors.toList());

        return new Event(
                UUID.fromString(doc.getId()),
                doc.getName(),
                doc.getDescription(),
                doc.getImageUrl(),
                UUID.fromString(doc.getOrganizerId()),
                venue,
                EventStatus.valueOf(doc.getStatus()),
                shows,
                doc.getCreatedAt(),
                doc.getUpdatedAt()
        );
    }

    public static Show toShowDomain(EventDocument.ShowDocument doc) {
        if (doc == null) return null;

        SaleWindow saleWindow = doc.getSaleWindow() != null
                ? new SaleWindow(
                doc.getSaleWindow().getOpensAt(),
                doc.getSaleWindow().getClosesAt())
                : null;

        List<TicketType> ticketTypes = doc.getTicketTypes().stream()
                .map(t -> new TicketType(
                        UUID.fromString(t.getId()),
                        t.getName(),
                        t.getDescription(),
                        t.getPrice(),
                        t.getCurrency(),
                        t.getTotalQuantity(),
                        t.getSortOrder()))
                .collect(Collectors.toList());

        return new Show(
                UUID.fromString(doc.getId()),
                UUID.fromString(doc.getEventId()),
                doc.getStartTime(),
                doc.getEndTime(),
                ShowStatus.valueOf(doc.getStatus()),
                saleWindow,
                ticketTypes
        );
    }
}
