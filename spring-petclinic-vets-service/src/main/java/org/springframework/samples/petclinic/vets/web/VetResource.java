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

import java.util.List;
import java.util.Set;

import jakarta.validation.Valid;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.samples.petclinic.vets.model.Vet;
import org.springframework.samples.petclinic.vets.model.VetRepository;
import org.springframework.samples.petclinic.vets.model.Specialty;
import org.springframework.samples.petclinic.vets.model.SpecialtyRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestBody;

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

    VetResource(VetRepository vetRepository, SpecialtyRepository specialtyRepository) {
        this.vetRepository = vetRepository;
        this.specialtyRepository = specialtyRepository;
    }

    @GetMapping
    @Cacheable("vets")
    public List<Vet> showResourcesVetList() {
        return vetRepository.findAll();
    }

    @GetMapping("/{vetId}")
    public Vet getVet(@PathVariable("vetId") int vetId) {
        return vetRepository.findById(vetId).orElseThrow(() -> new VetNotFoundException(vetId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @CacheEvict(cacheNames = "vets", allEntries = true)
    public Vet createVet(@Valid @RequestBody VetCreateRequest request) {
        Vet vet = new Vet();
        vet.setFirstName(request.firstName());
        vet.setLastName(request.lastName());

        if (request.specialtyIds() != null && !request.specialtyIds().isEmpty()) {
            List<Specialty> specs = specialtyRepository.findAllById(request.specialtyIds());
            specs.forEach(vet::addSpecialty);
        }

        return vetRepository.save(vet);
    }

    @PutMapping("/{vetId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @CacheEvict(cacheNames = "vets", allEntries = true)
    public void updateVet(@PathVariable("vetId") int vetId, @Valid @RequestBody VetCreateRequest request) {
        Vet vet = vetRepository.findById(vetId).orElseThrow(() -> new VetNotFoundException(vetId));
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
    public void deleteVet(@PathVariable("vetId") int vetId) {
        if (!vetRepository.existsById(vetId)) {
            throw new VetNotFoundException(vetId);
        }
        vetRepository.deleteById(vetId);
    }
}

record VetCreateRequest(String firstName, String lastName, Set<Integer> specialtyIds) {}

@ResponseStatus(HttpStatus.NOT_FOUND)
class VetNotFoundException extends RuntimeException {
    VetNotFoundException(int id) {
        super("Vet " + id + " not found");
    }
}
