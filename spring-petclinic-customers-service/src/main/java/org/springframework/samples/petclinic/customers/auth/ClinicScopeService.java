package org.springframework.samples.petclinic.customers.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.samples.petclinic.customers.model.Owner;
import org.springframework.samples.petclinic.customers.model.Pet;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

/**
 * Enforces clinic isolation when a valid Bearer JWT with {@code clinicId} is present.
 * Requests without JWT keep previous behavior (for tests / internal calls).
 */
@Component
public class ClinicScopeService {

	private final JwtTokenService jwtTokenService;

	public ClinicScopeService(JwtTokenService jwtTokenService) {
		this.jwtTokenService = jwtTokenService;
	}

	public Optional<UUID> clinicIdFromJwt(HttpServletRequest request) {
		if (request == null) {
			return Optional.empty();
		}
		return jwtTokenService.readClinicId(request.getHeader("Authorization"));
	}

	public void assertOwnerInClinicScope(Owner owner, HttpServletRequest request) {
		Optional<UUID> jwtClinic = clinicIdFromJwt(request);
		if (jwtClinic.isEmpty()) {
			return;
		}
		UUID cid = owner.getClinicId();
		if (cid == null || !cid.equals(jwtClinic.get())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Owner is outside your clinic scope");
		}
	}

	public void assertPetInClinicScope(Pet pet, HttpServletRequest request) {
		if (pet == null || pet.getOwner() == null) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Pet is outside your clinic scope");
		}
		assertOwnerInClinicScope(pet.getOwner(), request);
	}
}
