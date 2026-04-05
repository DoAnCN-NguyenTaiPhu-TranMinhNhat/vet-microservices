/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.vets.web;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.samples.petclinic.vets.model.Vet;
import org.springframework.samples.petclinic.vets.model.VetRepository;
import org.springframework.samples.petclinic.vets.model.Specialty;
import org.springframework.samples.petclinic.vets.model.SpecialtyRepository;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Ken Krebs
 * @author Arjen Poutsma
 * @author Maciej Szarlinski
 */
@RequestMapping("/vets")
@RestController
class VetResource {

    private final VetRepository vetRepository;
    private final SpecialtyRepository specialtyRepository;
    private final VetJwtSupport vetJwtSupport;

    VetResource(VetRepository vetRepository, SpecialtyRepository specialtyRepository, VetJwtSupport vetJwtSupport) {
        this.vetRepository = vetRepository;
        this.specialtyRepository = specialtyRepository;
        this.vetJwtSupport = vetJwtSupport;
    }

    @GetMapping
    public List<Vet> showResourcesVetList(
        @RequestHeader(value = "Authorization", required = false) String authorization) {
        Optional<UUID> clinicId = vetJwtSupport.readClinicId(authorization);
        if (isSingleVetStaff(authorization)) {
            UUID vid = vetJwtSupport.readVeterinarianId(authorization).orElseThrow();
            return vetRepository.findById(vid)
                .filter(v -> clinicId.isEmpty() || vetBelongsToClinic(v, clinicId.get()))
                .map(List::of)
                .orElseGet(Collections::emptyList);
        }
        if (clinicId.isPresent()) {
            return vetRepository.findByClinicIdOrderByLastNameAscFirstNameAsc(clinicId.get());
        }
        return vetRepository.findAll();
    }

    @GetMapping("/{vetId}")
    public Vet getVet(
        @PathVariable("vetId") UUID vetId,
        @RequestHeader(value = "Authorization", required = false) String authorization) {
        Vet vet = vetRepository.findById(vetId).orElseThrow(() -> new VetNotFoundException(vetId));
        assertVetVisibleInCallerClinic(authorization, vet);
        if (isSingleVetStaff(authorization)) {
            UUID ownId = vetJwtSupport.readVeterinarianId(authorization).orElseThrow();
            if (!ownId.equals(vetId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to access this veterinarian");
            }
        }
        return vet;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @CacheEvict(cacheNames = "vets", allEntries = true)
    public Vet createVet(
        @Valid @RequestBody VetCreateRequest request,
        @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (isSingleVetStaff(authorization)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Clinic login cannot create veterinarians");
        }
        Vet vet = new Vet();
        vet.setFirstName(request.firstName());
        vet.setLastName(request.lastName());
        vetJwtSupport.readClinicId(authorization).ifPresent(vet::setClinicId);

        if (request.specialtyIds() != null && !request.specialtyIds().isEmpty()) {
            List<Specialty> specs = specialtyRepository.findAllById(request.specialtyIds());
            specs.forEach(vet::addSpecialty);
        }

        return vetRepository.save(vet);
    }

    @PutMapping("/{vetId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @CacheEvict(cacheNames = "vets", allEntries = true)
    public void updateVet(
        @PathVariable("vetId") UUID vetId,
        @Valid @RequestBody VetCreateRequest request,
        @RequestHeader(value = "Authorization", required = false) String authorization) {
        Vet vet = vetRepository.findById(vetId).orElseThrow(() -> new VetNotFoundException(vetId));
        assertVetVisibleInCallerClinic(authorization, vet);
        if (isSingleVetStaff(authorization)) {
            UUID ownId = vetJwtSupport.readVeterinarianId(authorization).orElseThrow();
            if (!ownId.equals(vetId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to update this veterinarian");
            }
        }
        vet.setFirstName(request.firstName());
        vet.setLastName(request.lastName());
        Set<Specialty> specsSet = Set.of();
        if (request.specialtyIds() != null && !request.specialtyIds().isEmpty()) {
            specsSet = Set.copyOf(specialtyRepository.findAllById(request.specialtyIds()));
        }
        vet.setSpecialties(specsSet);
        vetRepository.save(vet);
    }

    @DeleteMapping("/{vetId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @CacheEvict(cacheNames = "vets", allEntries = true)
    public void deleteVet(
        @PathVariable("vetId") UUID vetId,
        @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (isSingleVetStaff(authorization)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Clinic login cannot delete veterinarians");
        }
        Vet vet = vetRepository.findById(vetId).orElseThrow(() -> new VetNotFoundException(vetId));
        assertVetVisibleInCallerClinic(authorization, vet);
        vetRepository.deleteById(vetId);
    }

    /** Staff linked to one vet row, without clinic-admin — restricted to that vet only. */
    private boolean isSingleVetStaff(String authorization) {
        return vetJwtSupport.readVeterinarianId(authorization).isPresent()
            && !vetJwtSupport.readClinicAdmin(authorization);
    }

    private static boolean vetBelongsToClinic(Vet v, UUID clinicId) {
        return v.getClinicId() != null && v.getClinicId().equals(clinicId);
    }

    private void assertVetVisibleInCallerClinic(String authorization, Vet vet) {
        Optional<UUID> clinicId = vetJwtSupport.readClinicId(authorization);
        if (clinicId.isPresent() && !vetBelongsToClinic(vet, clinicId.get())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vet not in your clinic");
        }
    }
}

record VetCreateRequest(String firstName, String lastName, Set<Integer> specialtyIds) {}

@ResponseStatus(HttpStatus.NOT_FOUND)
class VetNotFoundException extends RuntimeException {
    VetNotFoundException(UUID id) {
        super("Vet " + id + " not found");
    }
}
