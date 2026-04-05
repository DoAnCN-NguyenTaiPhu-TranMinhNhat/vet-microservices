package org.springframework.samples.petclinic.customers.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public final class ClinicAuthDtos {

	private ClinicAuthDtos() {
	}

	public record RegisterRequest(
		@NotBlank @Size(max = 120) String clinicName,
		@Size(max = 30) String clinicPhone,
		@Size(max = 255) String clinicAddress,
		@NotBlank @Email String email,
		@NotBlank @Size(min = 6, max = 72) String password,
		@NotBlank @Size(max = 80) String displayName,
		UUID veterinarianId
	) {
	}

	public record LoginRequest(
		@NotBlank @Email String email,
		@NotBlank String password
	) {
	}

	public record UserInfo(
		UUID id,
		String email,
		String displayName,
		UUID clinicId,
		String clinicName,
		UUID veterinarianId,
		boolean clinicAdmin
	) {
	}

	public record AuthResponse(
		String accessToken,
		String tokenType,
		long expiresIn,
		UserInfo user
	) {
	}
}
