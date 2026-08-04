package com.example.catalog.adapter.out.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "events")
@CompoundIndexes({
        @CompoundIndex(name = "status_shows_startTime_idx", def = "{'status': 1, 'shows.startTime': 1}")
})
public class EventDocument {

    @Id
    private String id;

    @TextIndexed(weight = 3.0f)
    private String name;

    @TextIndexed(weight = 1.0f)
    private String description;

    private String imageUrl;

    @Indexed
    private String organizerId;

    private VenueDocument venue;

    @Indexed
    private String status;

    private List<ShowDocument> shows = new ArrayList<>();

    private Instant createdAt;
    private Instant updatedAt;

    public EventDocument() {}

    public EventDocument(String id, String name, String description, String imageUrl, String organizerId, VenueDocument venue, String status, List<ShowDocument> shows, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.organizerId = organizerId;
        this.venue = venue;
        this.status = status;
        this.shows = shows != null ? shows : new ArrayList<>();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getOrganizerId() { return organizerId; }
    public void setOrganizerId(String organizerId) { this.organizerId = organizerId; }

    public VenueDocument getVenue() { return venue; }
    public void setVenue(VenueDocument venue) { this.venue = venue; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<ShowDocument> getShows() { return shows; }
    public void setShows(List<ShowDocument> shows) { this.shows = shows; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    // Nested documents
    public static class VenueDocument {
        private String name;
        private String address;
        private String city;
        private int capacity;

        public VenueDocument() {}

        public VenueDocument(String name, String address, String city, int capacity) {
            this.name = name;
            this.address = address;
            this.city = city;
            this.capacity = capacity;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }

        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }

        public int getCapacity() { return capacity; }
        public void setCapacity(int capacity) { this.capacity = capacity; }
    }

    public static class ShowDocument {
        private String id;
        private String eventId;
        private Instant startTime;
        private Instant endTime;
        private String status;
        private SaleWindowDocument saleWindow;
        private List<TicketTypeDocument> ticketTypes = new ArrayList<>();

        public ShowDocument() {}

        public ShowDocument(String id, String eventId, Instant startTime, Instant endTime, String status, SaleWindowDocument saleWindow, List<TicketTypeDocument> ticketTypes) {
            this.id = id;
            this.eventId = eventId;
            this.startTime = startTime;
            this.endTime = endTime;
            this.status = status;
            this.saleWindow = saleWindow;
            this.ticketTypes = ticketTypes != null ? ticketTypes : new ArrayList<>();
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getEventId() { return eventId; }
        public void setEventId(String eventId) { this.eventId = eventId; }

        public Instant getStartTime() { return startTime; }
        public void setStartTime(Instant startTime) { this.startTime = startTime; }

        public Instant getEndTime() { return endTime; }
        public void setEndTime(Instant endTime) { this.endTime = endTime; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public SaleWindowDocument getSaleWindow() { return saleWindow; }
        public void setSaleWindow(SaleWindowDocument saleWindow) { this.saleWindow = saleWindow; }

        public List<TicketTypeDocument> getTicketTypes() { return ticketTypes; }
        public void setTicketTypes(List<TicketTypeDocument> ticketTypes) { this.ticketTypes = ticketTypes; }
    }

    public static class SaleWindowDocument {
        private Instant opensAt;
        private Instant closesAt;

        public SaleWindowDocument() {}

        public SaleWindowDocument(Instant opensAt, Instant closesAt) {
            this.opensAt = opensAt;
            this.closesAt = closesAt;
        }

        public Instant getOpensAt() { return opensAt; }
        public void setOpensAt(Instant opensAt) { this.opensAt = opensAt; }

        public Instant getClosesAt() { return closesAt; }
        public void setClosesAt(Instant closesAt) { this.closesAt = closesAt; }
    }

    public static class TicketTypeDocument {
        private String id;
        private String name;
        private String description;
        private BigDecimal price;
        private String currency;
        private int totalQuantity;
        private int sortOrder;

        public TicketTypeDocument() {}

        public TicketTypeDocument(String id, String name, String description, BigDecimal price, String currency, int totalQuantity, int sortOrder) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.price = price;
            this.currency = currency;
            this.totalQuantity = totalQuantity;
            this.sortOrder = sortOrder;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }

        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }

        public int getTotalQuantity() { return totalQuantity; }
        public void setTotalQuantity(int totalQuantity) { this.totalQuantity = totalQuantity; }

        public int getSortOrder() { return sortOrder; }
        public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    }
}
