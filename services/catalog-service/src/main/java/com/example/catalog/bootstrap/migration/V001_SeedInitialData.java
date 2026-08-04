package com.example.catalog.bootstrap.migration;

import com.example.catalog.adapter.out.persistence.EventDocument;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@ChangeUnit(id = "seed-initial-catalog-data", order = "001", author = "antigravity")
public class V001_SeedInitialData {

    public static final String ORGANIZER_ID = "11111111-1111-1111-1111-111111111111";
    public static final String FLASH_SALE_EVENT_ID = "cccccccc-cccc-cccc-cccc-cccccccccccc";
    public static final String FLASH_SALE_SHOW_ID = "cccccccc-cccc-cccc-cccc-cccccccc0001";
    public static final String FLASH_SALE_TICKET_TYPE_ID = "cccccccc-cccc-cccc-cccc-cccccccc0002";

    @Execution
    public void execute(MongoTemplate mongoTemplate) {
        Instant now = Instant.now();

        // 1. Summer Music Festival
        EventDocument summerFest = new EventDocument(
                "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                "Summer Music Festival",
                "Grand outdoor summer music festival with top artists.",
                "https://images.unsplash.com/photo-1470225620780-dba8ba36b745",
                ORGANIZER_ID,
                new EventDocument.VenueDocument("Saigon Exhibition Center", "799 Nguyen Van Linh", "Ho Chi Minh", 10000),
                "PUBLISHED",
                List.of(
                        new EventDocument.ShowDocument(
                                "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa0001",
                                "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                                now.plus(30, ChronoUnit.DAYS),
                                now.plus(30, ChronoUnit.DAYS).plus(4, ChronoUnit.HOURS),
                                "ON_SALE",
                                new EventDocument.SaleWindowDocument(now.minus(1, ChronoUnit.DAYS), now.plus(30, ChronoUnit.DAYS).minus(2, ChronoUnit.HOURS)),
                                List.of(
                                        new EventDocument.TicketTypeDocument("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa0011", "VIP", "VIP Lounge Access", new BigDecimal("2000000"), "VND", 100, 1),
                                        new EventDocument.TicketTypeDocument("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa0012", "Standard", "General Standing", new BigDecimal("500000"), "VND", 500, 2),
                                        new EventDocument.TicketTypeDocument("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa0013", "Early Bird", "Discounted General Standing", new BigDecimal("350000"), "VND", 200, 3)
                                )
                        ),
                        new EventDocument.ShowDocument(
                                "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa0002",
                                "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                                now.plus(31, ChronoUnit.DAYS),
                                now.plus(31, ChronoUnit.DAYS).plus(4, ChronoUnit.HOURS),
                                "SCHEDULED",
                                new EventDocument.SaleWindowDocument(now.minus(1, ChronoUnit.DAYS), now.plus(31, ChronoUnit.DAYS).minus(2, ChronoUnit.HOURS)),
                                List.of(
                                        new EventDocument.TicketTypeDocument("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa0021", "VIP", "VIP Lounge Access", new BigDecimal("2000000"), "VND", 100, 1),
                                        new EventDocument.TicketTypeDocument("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa0022", "Standard", "General Standing", new BigDecimal("500000"), "VND", 500, 2)
                                )
                        )
                ),
                now,
                now
        );

        // 2. Tech Conference 2026
        EventDocument techConf = new EventDocument(
                "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                "Tech Conference 2026",
                "The premier conference on software architecture, microservices, and AI.",
                "https://images.unsplash.com/photo-1540575467063-178a50c2df87",
                ORGANIZER_ID,
                new EventDocument.VenueDocument("National Convention Center", "Pham Hung", "Hanoi", 3500),
                "PUBLISHED",
                List.of(
                        new EventDocument.ShowDocument(
                                "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0001",
                                "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                                now.plus(45, ChronoUnit.DAYS),
                                now.plus(45, ChronoUnit.DAYS).plus(8, ChronoUnit.HOURS),
                                "ON_SALE",
                                new EventDocument.SaleWindowDocument(now.minus(1, ChronoUnit.DAYS), now.plus(45, ChronoUnit.DAYS).minus(2, ChronoUnit.HOURS)),
                                List.of(
                                        new EventDocument.TicketTypeDocument("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0011", "Premium", "Front Row + Networking Lunch", new BigDecimal("1500000"), "VND", 50, 1),
                                        new EventDocument.TicketTypeDocument("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0012", "Regular", "Standard Conference Seat", new BigDecimal("800000"), "VND", 300, 2)
                                )
                        )
                ),
                now,
                now
        );

        // 3. Flash Sale Concert (Target for Step 04 & 05 Flash Sale tests)
        EventDocument flashSaleConcert = new EventDocument(
                FLASH_SALE_EVENT_ID,
                "Flash Sale Concert",
                "Exclusive live concert with flash sale limited inventory.",
                "https://images.unsplash.com/photo-1501386761578-eac5c94b800a",
                ORGANIZER_ID,
                new EventDocument.VenueDocument("Hoa Binh Theater", "240 Ba Thang Hai", "Ho Chi Minh", 2500),
                "PUBLISHED",
                List.of(
                        new EventDocument.ShowDocument(
                                FLASH_SALE_SHOW_ID,
                                FLASH_SALE_EVENT_ID,
                                now.plus(15, ChronoUnit.DAYS),
                                now.plus(15, ChronoUnit.DAYS).plus(3, ChronoUnit.HOURS),
                                "ON_SALE",
                                new EventDocument.SaleWindowDocument(now.minus(1, ChronoUnit.DAYS), now.plus(15, ChronoUnit.DAYS).minus(1, ChronoUnit.HOURS)),
                                List.of(
                                        new EventDocument.TicketTypeDocument(FLASH_SALE_TICKET_TYPE_ID, "General Admission", "Flash sale admission ticket", new BigDecimal("250000"), "VND", 100, 1)
                                )
                        )
                ),
                now,
                now
        );

        mongoTemplate.save(summerFest);
        mongoTemplate.save(techConf);
        mongoTemplate.save(flashSaleConcert);
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        mongoTemplate.remove(new org.springframework.data.mongodb.core.query.Query(), EventDocument.class);
    }
}
