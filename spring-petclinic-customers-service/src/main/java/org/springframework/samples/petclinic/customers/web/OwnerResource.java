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
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.samples.petclinic.customers.auth.ClinicScopeService;
import org.springframework.samples.petclinic.customers.web.mapper.OwnerEntityMapper;
import org.springframework.samples.petclinic.customers.model.Owner;
import org.springframework.samples.petclinic.customers.model.OwnerRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 * @author Michael Isvy
 * @author Maciej Szarlinski
 */
@RequestMapping("/owners")
@RestController
@Timed("petclinic.owner")
class OwnerResource {

    private static final Logger log = LoggerFactory.getLogger(OwnerResource.class);

    private final OwnerRepository ownerRepository;
    private final OwnerEntityMapper ownerEntityMapper;
    private final ClinicScopeService clinicScopeService;

    OwnerResource(
        OwnerRepository ownerRepository,
        OwnerEntityMapper ownerEntityMapper,
        ClinicScopeService clinicScopeService) {
        this.ownerRepository = ownerRepository;
        this.ownerEntityMapper = ownerEntityMapper;
        this.clinicScopeService = clinicScopeService;
    }

    /**
     * Create Owner
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Owner createOwner(@Valid @RequestBody OwnerRequest ownerRequest, HttpServletRequest request) {
        Owner owner = ownerEntityMapper.map(new Owner(), ownerRequest);
        Optional<UUID> jwtClinic = clinicScopeService.clinicIdFromJwt(request);
        jwtClinic.ifPresent(owner::setClinicId);
        return ownerRepository.save(owner);
    }

    /**
     * Read single Owner
     */
    @GetMapping(value = "/{ownerId}")
    public Owner findOwner(@PathVariable("ownerId") UUID ownerId, HttpServletRequest request) {
        Owner owner = ownerRepository.findById(ownerId)
            .orElseThrow(() -> new ResourceNotFoundException("Owner " + ownerId + " not found"));
        clinicScopeService.assertOwnerInClinicScope(owner, request);
        return owner;
    }

	/**
	 * Read List of Owners. When {@code clinicId} is set, only owners for that clinic (e.g. logged-in clinic).
	 */
	@GetMapping
	public List<Owner> findAll(
		@RequestParam(name = "clinicId", required = false) UUID clinicId,
		HttpServletRequest request) {
		Optional<UUID> jwtClinic = clinicScopeService.clinicIdFromJwt(request);
		if (jwtClinic.isPresent()) {
			return ownerRepository.findByClinicIdOrderByLastNameAscFirstNameAsc(jwtClinic.get());
		}
		if (clinicId != null) {
			return ownerRepository.findByClinicIdOrderByLastNameAscFirstNameAsc(clinicId);
		}
		return ownerRepository.findAll();
	}

    /**
     * Update Owner
     */
    @PutMapping(value = "/{ownerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateOwner(
        @PathVariable("ownerId") UUID ownerId,
        @Valid @RequestBody OwnerRequest ownerRequest,
        HttpServletRequest request) {
        final Owner ownerModel = ownerRepository.findById(ownerId).orElseThrow(() -> new ResourceNotFoundException("Owner " + ownerId + " not found"));
        clinicScopeService.assertOwnerInClinicScope(ownerModel, request);
        UUID clinicBefore = ownerModel.getClinicId();

        ownerEntityMapper.map(ownerModel, ownerRequest);
        ownerModel.setClinicId(clinicBefore);
        log.info("Saving owner {}", ownerModel);
        ownerRepository.save(ownerModel);
    }

    /**
     * Delete Owner
     */
    @DeleteMapping(value = "/{ownerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOwner(@PathVariable("ownerId") UUID ownerId, HttpServletRequest request) {
        Owner owner = ownerRepository.findById(ownerId)
            .orElseThrow(() -> new ResourceNotFoundException("Owner " + ownerId + " not found"));
        clinicScopeService.assertOwnerInClinicScope(owner, request);
        ownerRepository.deleteById(ownerId);
    }
}
