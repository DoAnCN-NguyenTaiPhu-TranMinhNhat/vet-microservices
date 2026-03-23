package org.springframework.samples.petclinic.api.boundary.web;

import org.springframework.http.HttpStatus;
import org.springframework.samples.petclinic.api.application.CustomersServiceClient;
import org.springframework.samples.petclinic.api.dto.OwnerDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * P0 Orchestration endpoints.
 *
 * Frontend must call API Gateway only. These endpoints perform sync-delete:
 * - Delete Pet => delete related Visits => delete Pet
 * - Delete Owner => delete related Visits for all pets => delete Owner
 */
@RestController
@RequestMapping("/api/orch")
public class OrchestrationController {

    private final WebClient.Builder webClientBuilder;
    private final CustomersServiceClient customersServiceClient;

    public OrchestrationController(WebClient.Builder webClientBuilder, CustomersServiceClient customersServiceClient) {
        this.webClientBuilder = webClientBuilder;
        this.customersServiceClient = customersServiceClient;
    }

    @DeleteMapping("/owners/{ownerId}/pets/{petId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deletePetSync(@PathVariable int ownerId, @PathVariable int petId) {
        return deleteVisitsByPetId(petId)
            .onErrorResume(e -> Mono.empty()) // if visits-service is down, still try to delete pet
            .then(deletePet(ownerId, petId)
                // Idempotency: deleting an already-deleted pet should still be 204
                .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.empty()));
    }

    @DeleteMapping("/owners/{ownerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteOwnerSync(@PathVariable int ownerId) {
        Mono<OwnerDetails> ownerMono = customersServiceClient.getOwner(ownerId)
            // Idempotency: if owner already deleted, just return 204
            .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.empty());

        Mono<Void> deleteAllVisits = ownerMono
            .flatMapMany(owner -> Flux.fromIterable(owner.getPetIds()))
            // sequentially delete visits to avoid overloading
            .concatMap(this::deleteVisitsByPetId)
            .onErrorResume(e -> Mono.empty())
            .then();

        return deleteAllVisits.then(deleteOwner(ownerId)
            // Idempotency: deleting an already-deleted owner should still be 204
            .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.empty()));
    }

    private Mono<Void> deleteVisitsByPetId(int petId) {
        return webClientBuilder.build()
            .delete()
            // Service Kubernetes đang expose port 8082
            .uri("http://visits-service:8082/pets/{petId}/visits", petId)
            .retrieve()
            .bodyToMono(Void.class);
    }

    private Mono<Void> deletePet(int ownerId, int petId) {
        return webClientBuilder.build()
            .delete()
            // Service Kubernetes đang expose port 8081
            .uri("http://customers-service:8081/owners/{ownerId}/pets/{petId}", ownerId, petId)
            .retrieve()
            .bodyToMono(Void.class);
    }

    private Mono<Void> deleteOwner(int ownerId) {
        return webClientBuilder.build()
            .delete()
            // Service Kubernetes đang expose port 8081
            .uri("http://customers-service:8081/owners/{ownerId}", ownerId)
            .retrieve()
            .bodyToMono(Void.class);
    }
}

