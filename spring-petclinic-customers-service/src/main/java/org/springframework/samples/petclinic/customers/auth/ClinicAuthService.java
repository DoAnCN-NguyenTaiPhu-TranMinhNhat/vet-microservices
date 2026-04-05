package org.springframework.samples.petclinic.customers.auth;

import org.springframework.http.HttpStatus;
import org.springframework.samples.petclinic.customers.model.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class ClinicAuthService {

	private final ClinicRepository clinicRepository;
	private final ClinicUserRepository clinicUserRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenService jwtTokenService;

	public ClinicAuthService(ClinicRepository clinicRepository,
		ClinicUserRepository clinicUserRepository,
		PasswordEncoder passwordEncoder,
		JwtTokenService jwtTokenService) {
		this.clinicRepository = clinicRepository;
		this.clinicUserRepository = clinicUserRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtTokenService = jwtTokenService;
	}

	@Transactional
	public ClinicAuthDtos.AuthResponse register(ClinicAuthDtos.RegisterRequest req) {
		String email = req.email().trim().toLowerCase();
		if (clinicUserRepository.existsByEmailIgnoreCase(email)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
		}
		Clinic clinic = new Clinic();
		clinic.setName(req.clinicName().trim());
		clinic.setPhone(trimToNull(req.clinicPhone()));
		clinic.setAddress(trimToNull(req.clinicAddress()));
		clinic = clinicRepository.save(clinic);

		ClinicUser user = new ClinicUser();
		user.setClinic(clinic);
		user.setEmail(email);
		user.setPasswordHash(passwordEncoder.encode(req.password()));
		user.setDisplayName(req.displayName().trim());
		user.setVeterinarianId(req.veterinarianId());
		user.setClinicAdmin(true);
		user = clinicUserRepository.save(user);

		return buildResponse(user);
	}

	public ClinicAuthDtos.AuthResponse login(ClinicAuthDtos.LoginRequest req) {
		String email = req.email().trim().toLowerCase();
		ClinicUser user = clinicUserRepository.findByEmailIgnoreCase(email)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
		if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
		}
		return buildResponse(user);
	}

	/**
	 * Current clinic user from DB (fresh {@code clinicAdmin}, etc.) — works with older JWTs missing newer claims.
	 */
	public ClinicAuthDtos.UserInfo me(String authorizationHeader) {
		UUID userId = jwtTokenService.readUserId(authorizationHeader)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid token"));
		ClinicUser user = clinicUserRepository.findById(userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
		return toUserInfo(user);
	}

	private ClinicAuthDtos.UserInfo toUserInfo(ClinicUser user) {
		return new ClinicAuthDtos.UserInfo(
			user.getId(),
			user.getEmail(),
			user.getDisplayName(),
			user.getClinic().getId(),
			user.getClinic().getName(),
			user.getVeterinarianId(),
			user.isClinicAdmin()
		);
	}

	private ClinicAuthDtos.AuthResponse buildResponse(ClinicUser user) {
		String token = jwtTokenService.createToken(user);
		long expiresSeconds = Math.max(1L, jwtTokenService.getExpirationMs() / 1000);
		return new ClinicAuthDtos.AuthResponse(token, "Bearer", expiresSeconds, toUserInfo(user));
	}

	private static String trimToNull(String s) {
		if (s == null) {
			return null;
		}
		String t = s.trim();
		return t.isEmpty() ? null : t;
	}
}
