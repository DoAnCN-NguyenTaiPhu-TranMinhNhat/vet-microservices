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
package org.springframework.samples.petclinic.customers.web;

import io.micrometer.core.annotation.Timed;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.samples.petclinic.customers.auth.ClinicScopeService;
import org.springframework.samples.petclinic.customers.model.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 * @author Maciej Szarlinski
 * @author Ramazan Sakin
 */
@RestController
@Timed("petclinic.pet")
class PetResource {

    private static final Logger log = LoggerFactory.getLogger(PetResource.class);

    private final PetRepository petRepository;
    private final OwnerRepository ownerRepository;
    private final ClinicScopeService clinicScopeService;

    PetResource(PetRepository petRepository, OwnerRepository ownerRepository, ClinicScopeService clinicScopeService) {
        this.petRepository = petRepository;
        this.ownerRepository = ownerRepository;
        this.clinicScopeService = clinicScopeService;
    }

    @GetMapping("/petTypes")
    public List<PetType> getPetTypes() {
        return petRepository.findPetTypes();
    }

    @PostMapping("/owners/{ownerId}/pets")
    @ResponseStatus(HttpStatus.CREATED)
    public Pet processCreationForm(
        @RequestBody PetCreateRequest petCreateRequest,
        @PathVariable("ownerId") UUID ownerId,
        HttpServletRequest request) {

        Owner owner = ownerRepository.findById(ownerId)
            .orElseThrow(() -> new ResourceNotFoundException("Owner " + ownerId + " not found"));
        clinicScopeService.assertOwnerInClinicScope(owner, request);

        final Pet pet = new Pet();
        owner.addPet(pet);
        return save(pet, petCreateRequest.toPetRequest());
    }

    @PutMapping("/owners/*/pets/{petId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void processUpdateForm(@RequestBody PetRequest petRequest, HttpServletRequest request) {
        UUID petId = petRequest.id();
        Pet pet = findPetById(petId);
        clinicScopeService.assertPetInClinicScope(pet, request);
        save(pet, petRequest);
    }

    @DeleteMapping("/owners/{ownerId}/pets/{petId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePet(
        @PathVariable("ownerId") UUID ownerId,
        @PathVariable("petId") UUID petId,
        HttpServletRequest request
    ) {
        Owner owner = ownerRepository.findById(ownerId)
            .orElseThrow(() -> new ResourceNotFoundException("Owner " + ownerId + " not found"));
        clinicScopeService.assertOwnerInClinicScope(owner, request);

        Pet pet = petRepository.findById(petId)
            .orElseThrow(() -> new ResourceNotFoundException("Pet " + petId + " not found"));

        if (pet.getOwner() == null || pet.getOwner().getId() == null || !pet.getOwner().getId().equals(owner.getId())) {
            throw new ResourceNotFoundException("Pet " + petId + " not found for owner " + ownerId);
        }

        int deleted = petRepository.hardDeleteById(petId);
        if (deleted == 0) {
            throw new ResourceNotFoundException("Pet " + petId + " not found");
        }
    }

    private Pet save(final Pet pet, final PetRequest petRequest) {

        int typeId = petRequest.typeId();
        if (typeId != 1 && typeId != 2) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Only cat (typeId=1) and dog (typeId=2) are supported"
            );
        }

        pet.setName(petRequest.name());
        pet.setBirthDate(petRequest.birthDate());

        petRepository.findPetTypeById(typeId)
            .ifPresent(pet::setType);

        // Save AI fields
        pet.setGender(petRequest.gender());
        pet.setVaccinationStatus(petRequest.vaccinationStatus());
        pet.setMedicalNotes(petRequest.medicalNotes());

        log.info("Saving pet {}", pet);
        return petRepository.save(pet);
    }

    @GetMapping("owners/*/pets/{petId}")
    public PetDetails findPet(@PathVariable("petId") UUID petId, HttpServletRequest request) {
        Pet pet = findPetById(petId);
        clinicScopeService.assertPetInClinicScope(pet, request);
        return new PetDetails(pet);
    }


    private Pet findPetById(UUID petId) {
        return petRepository.findById(petId)
            .orElseThrow(() -> new ResourceNotFoundException("Pet " + petId + " not found"));
    }

}
