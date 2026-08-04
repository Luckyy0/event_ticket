package com.example.gateway.route;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class RouteConfigTest {

    @Autowired
    private RouteLocator routeLocator;

    @Test
    void shouldDefineAllRequiredRoutes() {
        StepVerifier.create(routeLocator.getRoutes())
                .recordWith(java.util.ArrayList::new)
                .thenConsumeWhile(route -> true)
                .consumeRecordedWith(routes -> {
                    var routeIds = routes.stream().map(r -> r.getId()).toList();
                    assertThat(routeIds).contains(
                            "catalog-read",
                            "catalog-write",
                            "inventory-reservation",
                            "inventory-general",
                            "orders",
                            "payment-webhook",
                            "payments",
                            "tickets",
                            "checkin",
                            "user-profile",
                            "admin"
                    );
                })
                .verifyComplete();
    }
}
