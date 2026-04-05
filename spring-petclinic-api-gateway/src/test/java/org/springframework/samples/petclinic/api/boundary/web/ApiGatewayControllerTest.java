package org.springframework.samples.petclinic.api.boundary.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.samples.petclinic.api.application.CustomersServiceClient;
import org.springframework.samples.petclinic.api.application.VisitsServiceClient;
import org.springframework.samples.petclinic.api.config.GatewaySecurityConfiguration;
import org.springframework.samples.petclinic.api.dto.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@WebFluxTest(controllers = ApiGatewayController.class)
@Import({ReactiveResilience4JAutoConfiguration.class, CircuitBreakerConfiguration.class, GatewaySecurityConfiguration.class})
class ApiGatewayControllerTest {

    @MockBean
    private CustomersServiceClient customersServiceClient;

    @MockBean
    private VisitsServiceClient visitsServiceClient;

    @Autowired
    private WebTestClient client;


    @Test
    void getOwnerDetails_withAvailableVisitsService() {
        UUID ownerId = UUID.fromString("2ba55f38-4ab6-55d6-8011-ebc815a42531");
        UUID petId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        PetDetails cat = PetDetails.PetDetailsBuilder.aPetDetails()
            .id(petId)
            .name("Garfield")
            .visits(new ArrayList<>())
            .build();
        OwnerDetails owner = OwnerDetails.OwnerDetailsBuilder.anOwnerDetails()
            .id(ownerId)
            .pets(List.of(cat))
            .build();
        Mockito
            .when(customersServiceClient.getOwner(ownerId, null))
            .thenReturn(Mono.just(owner));

        VisitDetails visit = new VisitDetails(300, petId, null, "First visit");
        Visits visits = new Visits(List.of(visit));
        Mockito
            .when(visitsServiceClient.getVisitsForPets(Collections.singletonList(cat.id())))
            .thenReturn(Mono.just(visits));

        client.mutateWith(mockJwt())
            .get()
            .uri("/api/owners/" + ownerId)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.pets[0].name").isEqualTo("Garfield")
            .jsonPath("$.pets[0].visits[0].description").isEqualTo("First visit");
    }

    /**
     * Test Resilience4j fallback method
     */
    @Test
    void getOwnerDetails_withServiceError() {
        UUID ownerId = UUID.fromString("2ba55f38-4ab6-55d6-8011-ebc815a42531");
        UUID petId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        PetDetails cat = PetDetails.PetDetailsBuilder.aPetDetails()
            .id(petId)
            .name("Garfield")
            .visits(new ArrayList<>())
            .build();
        OwnerDetails owner = OwnerDetails.OwnerDetailsBuilder.anOwnerDetails()
            .id(ownerId)
            .pets(List.of(cat))
            .build();
        Mockito
            .when(customersServiceClient.getOwner(ownerId, null))
            .thenReturn(Mono.just(owner));

        Mockito
            .when(visitsServiceClient.getVisitsForPets(Collections.singletonList(cat.id())))
            .thenReturn(Mono.error(new ConnectException("Simulate error")));

        client.mutateWith(mockJwt())
            .get()
            .uri("/api/owners/" + ownerId)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.pets[0].name").isEqualTo("Garfield")
            .jsonPath("$.pets[0].visits").isEmpty();
    }

}
