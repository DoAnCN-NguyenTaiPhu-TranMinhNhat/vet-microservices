package org.springframework.samples.petclinic.api.application;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.samples.petclinic.api.dto.Visits;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class VisitsServiceClientIntegrationTest {

    private static final UUID PET_ID = UUID.fromString("f4dad75f-eebf-549f-8459-4ef9798de80f");

    private VisitsServiceClient visitsServiceClient;

    private MockWebServer server;

    @BeforeEach
    void setUp() {
        server = new MockWebServer();
        visitsServiceClient = new VisitsServiceClient(WebClient.builder());
        visitsServiceClient.setHostname(server.url("/").toString());
    }

    @AfterEach
    void shutdown() throws IOException {
        this.server.shutdown();
    }

    @Test
    void getVisitsForPets_withAvailableVisitsService() {
        prepareResponse(response -> response
            .setHeader("Content-Type", "application/json")
            .setBody("{\"items\":[{\"id\":5,\"date\":\"2018-11-15\",\"description\":\"test visit\",\"petId\":\"" + PET_ID + "\"}]}"));

        Mono<Visits> visits = visitsServiceClient.getVisitsForPets(Collections.singletonList(PET_ID));

        assertVisitDescriptionEquals(visits.block(), PET_ID,"test visit");
    }


    private void assertVisitDescriptionEquals(Visits visits, UUID petId, String description) {
        assertEquals(1, visits.items().size());
        assertNotNull(visits.items().get(0));
        assertEquals(petId, visits.items().get(0).petId());
        assertEquals(description, visits.items().get(0).description());
    }

    private void prepareResponse(Consumer<MockResponse> consumer) {
        MockResponse response = new MockResponse();
        consumer.accept(response);
        this.server.enqueue(response);
    }

}
